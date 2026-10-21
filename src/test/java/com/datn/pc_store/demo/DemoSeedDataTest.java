package com.datn.pc_store.demo;

import org.junit.jupiter.api.*;
import org.springframework.core.io.*;
import org.springframework.core.io.support.*;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.regex.*;

import static org.assertj.core.api.Assertions.assertThat;

/** Executes the original migrations and opt-in sample data in an isolated database. */
class DemoSeedDataTest {
    private static Connection connection;

    @BeforeAll
    static void createSampleDatabase() throws Exception {
        connection = DriverManager.getConnection("jdbc:h2:mem:demo-seed-validation;MODE=MySQL", "sa", "");
        Resource[] migrations = new PathMatchingResourcePatternResolver().getResources("classpath:db/migration/V*__*.sql");
        Arrays.sort(migrations, Comparator.comparingInt(resource ->
                Integer.parseInt(Objects.requireNonNull(resource.getFilename()).split("__")[0].substring(1))));
        for (Resource resource : migrations) {
            boolean priceMigration = resource.getFilename().startsWith("V13__");
            if (priceMigration) prepareLegacyPriceFixtures();
            String sql = resource.getContentAsString(StandardCharsets.UTF_8);
            // Adapt only MySQL-specific DDL syntax/index namespaces for H2; originals remain untouched.
            sql = sql.replaceAll("(?i)\\)\\s*ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;", ");")
                    .replace("DROP CHECK chk_users_role", "DROP CONSTRAINT chk_users_role")
                    .replaceAll("(?i) AFTER \\w+", "")
                    .replace("UTC_TIMESTAMP(6)", "CURRENT_TIMESTAMP(6)");
            Matcher indexes = Pattern.compile("(?i)CREATE INDEX (\\w+) ON (\\w+)").matcher(sql);
            sql = indexes.replaceAll(match -> "CREATE INDEX " + match.group(2) + "_" + match.group(1) + " ON " + match.group(2));
            execute(sql);
            if (priceMigration) verifyLegacyPriceBackfill();
        }
        execute(new ClassPathResource("db/demo/V14__seed_demo_data.sql").getContentAsString(StandardCharsets.UTF_8));
    }

    @AfterAll
    static void closeDatabase() throws Exception {
        if (connection != null) connection.close();
    }

    @Test
    void everyBusinessTableHasTenToTwentyRecordsAndTotalIsFourHundred() throws Exception {
        List<String> tables = new ArrayList<>();
        try (ResultSet result = connection.getMetaData().getTables(null, "PUBLIC", "%", new String[]{"BASE TABLE"})) {
            while (result.next()) tables.add(result.getString("TABLE_NAME"));
        }
        assertThat(tables).hasSize(32);
        long total = 0;
        for (String table : tables) {
            long rows = count("SELECT COUNT(*) FROM " + table);
            assertThat(rows).as("Record count of %s", table).isBetween(10L, 20L);
            total += rows;
        }
        assertThat(total).isEqualTo(400);
    }

    @Test
    void sampleAccountsCanAuthenticateAndDoNotHaveActiveSeedSessions() throws Exception {
        var encoder = new BCryptPasswordEncoder();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT password_hash FROM users")) {
            while (result.next()) assertThat(encoder.matches("Demo@123456", result.getString(1))).isTrue();
        }
        assertThat(count("SELECT COUNT(*) FROM users WHERE role='CUSTOMER'")).isEqualTo(10);
        assertThat(count("SELECT COUNT(DISTINCT role) FROM users")).isEqualTo(6);
        assertThat(count("SELECT COUNT(*) FROM refresh_tokens WHERE revoked=0")).isZero();
    }

    @Test
    void purchasingTotalsAndReceivedQuantitiesMatchConfirmedReceipts() throws Exception {
        assertThat(count("""
                SELECT COUNT(*) FROM products p JOIN product_price pp ON pp.product_id=p.id
                WHERE pp.is_present_price=1 AND p.price<>pp.price
                """)).isZero();
        assertThat(count("SELECT COUNT(*) FROM purchase_order_items WHERE subtotal <> quantity_order * unit_cost OR quantity_received > quantity_order")).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM purchase_orders po
                WHERE po.total_amount <> (SELECT SUM(i.subtotal) FROM purchase_order_items i WHERE i.purchase_order_id=po.id)
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM purchase_order_items i
                WHERE i.quantity_received <> COALESCE((SELECT SUM(ri.quantity_received)
                    FROM receipt_items ri JOIN goods_receipts gr ON gr.id=ri.goods_receipt_id
                    WHERE ri.purchase_order_item_id=i.id AND gr.status=1),0)
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM purchase_orders po JOIN purchase_order_items i ON i.purchase_order_id=po.id
                WHERE po.status=3 AND i.quantity_received<>i.quantity_order
                """)).isZero();
        assertThat(count("SELECT COUNT(DISTINCT status) FROM purchase_orders")).isEqualTo(4);
    }

    @Test
    void serialCountsAndProductsMatchTheirReceiptLines() throws Exception {
        assertThat(count("SELECT COUNT(*) FROM receipt_items WHERE quantity_received<>quantity_accepted+quantity_defective")).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM receipt_items ri
                WHERE ri.quantity_received<>(SELECT COUNT(*) FROM product_serials s WHERE s.receipt_item_id=ri.id)
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM product_serials s JOIN receipt_items ri ON ri.id=s.receipt_item_id
                JOIN purchase_order_items i ON i.id=ri.purchase_order_item_id WHERE s.product_id<>i.product_id
                """)).isZero();
    }

    @Test
    void salesInvoicesPaymentsAndAllocatedSerialsBalance() throws Exception {
        assertThat(count("SELECT COUNT(*) FROM order_items WHERE subtotal<>quantity*unit_price-discount_amount")).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM orders o WHERE o.final_amount<>o.total_amount-o.discount_amount
                   OR o.final_amount<>(SELECT SUM(i.subtotal) FROM order_items i WHERE i.order_id=o.id)
                   OR o.total_amount<>(SELECT SUM(i.quantity*i.unit_price) FROM order_items i WHERE i.order_id=o.id)
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM invoices i JOIN orders o ON o.id=i.order_id
                WHERE i.amount<>i.total_amount-i.discount_amount+i.tax_amount OR i.amount<>o.final_amount
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM orders o
                WHERE o.final_amount<>(SELECT SUM(p.amount) FROM payment p WHERE p.order_id=o.id AND p.status=1)
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM order_items i
                WHERE i.quantity<>(SELECT COUNT(*) FROM order_item_serial os WHERE os.order_item_id=i.id)
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM order_item_serial os JOIN order_items i ON i.id=os.order_item_id
                JOIN product_serials s ON s.id=os.product_serial_id WHERE s.product_id<>i.product_id
                """)).isZero();
    }

    @Test
    void warrantyReplacementsAndSupplierReturnsHaveMatchingOwnershipAndProducts() throws Exception {
        assertThat(count("""
                SELECT COUNT(*) FROM warranty_request wr JOIN warranty w ON w.id=wr.warranty_id
                JOIN order_item_serial os ON os.id=w.order_item_serial_id JOIN order_items i ON i.id=os.order_item_id
                JOIN orders o ON o.id=i.order_id WHERE wr.customer_id<>o.user_id
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM warranty_parts wp JOIN warranty_part_serial ws ON ws.warranty_part_id=wp.id
                JOIN product_serials s ON s.id=ws.product_serial_id
                WHERE wp.product_id<>s.product_id OR s.status<>8
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM warranty_parts wp JOIN warranty_request wr ON wr.id=wp.warranty_request_id
                JOIN warranty w ON w.id=wr.warranty_id JOIN order_item_serial os ON os.id=w.order_item_serial_id
                JOIN product_serials s ON s.id=os.product_serial_id WHERE s.product_id<>wp.product_id
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM purchase_return_items pri JOIN product_serials s ON s.id=pri.product_serial_id
                JOIN receipt_items ri ON ri.id=s.receipt_item_id JOIN goods_receipts gr ON gr.id=ri.goods_receipt_id
                JOIN purchase_orders po ON po.id=gr.purchase_order_id JOIN purchase_returns pr ON pr.id=pri.purchase_return_id
                WHERE pr.supplier_id<>po.supplier_id OR s.status<>5 OR pri.unit_cost<>ri.actual_unit_cost
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM purchase_returns pr
                WHERE pr.total_amount<>(SELECT SUM(i.unit_cost) FROM purchase_return_items i WHERE i.purchase_return_id=pr.id)
                """)).isZero();
    }

    @Test
    void customersHaveCartsAndDeliveriesReferenceRealResources() throws Exception {
        assertThat(count("""
                SELECT COUNT(*) FROM carts c JOIN users u ON u.id=c.customer_id
                WHERE u.role<>'CUSTOMER' OR c.quantity<>(SELECT SUM(i.quantity) FROM cart_items i WHERE i.cart_id=c.id)
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM delivery d WHERE
                    (d.resource_type='ORDER' AND NOT EXISTS(SELECT 1 FROM orders o WHERE o.id=d.resource_id)) OR
                    (d.resource_type='PURCHASE_RETURN' AND NOT EXISTS(SELECT 1 FROM purchase_returns pr WHERE pr.id=d.resource_id))
                """)).isZero();
        assertThat(count("""
                SELECT COUNT(*) FROM product_reviews r WHERE NOT EXISTS(
                    SELECT 1 FROM orders o JOIN order_items i ON i.order_id=o.id
                    WHERE o.user_id=r.customer_id AND i.product_id=r.product_id AND o.status=4)
                """)).isZero();
    }

    private static void prepareLegacyPriceFixtures() {
        execute("""
                INSERT INTO products(id,name,code) VALUES
                (9001,'Migration check 1','MIGRATION-CHECK-1'), (9002,'Migration check 2','MIGRATION-CHECK-2'),
                (9003,'Migration check 3','MIGRATION-CHECK-3'), (9004,'Migration check 4','MIGRATION-CHECK-4');
                INSERT INTO product_price(id,product_id,price,effective_from,effective_to,is_present_price) VALUES
                (9001,9001,100,DATEADD('DAY',-2,CURRENT_TIMESTAMP),DATEADD('DAY',-1,CURRENT_TIMESTAMP),0),
                (9002,9001,200,DATEADD('DAY',-1,CURRENT_TIMESTAMP),NULL,1),
                (9003,9003,300,DATEADD('DAY',-2,CURRENT_TIMESTAMP),DATEADD('DAY',-1,CURRENT_TIMESTAMP),1),
                (9004,9003,400,DATEADD('DAY',1,CURRENT_TIMESTAMP),NULL,1),
                (9005,9004,500,DATEADD('DAY',-1,CURRENT_TIMESTAMP),NULL,1),
                (9006,9004,600,DATEADD('DAY',-1,CURRENT_TIMESTAMP),NULL,1);
                """);
    }

    private static void verifyLegacyPriceBackfill() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT price FROM products WHERE id BETWEEN 9001 AND 9004 ORDER BY id")) {
            for (Long expected : new Long[]{200L, null, null, 600L}) {
                assertThat(result.next()).isTrue();
                assertThat(result.getObject(1, Long.class)).isEqualTo(expected);
            }
        }
        execute("DELETE FROM products WHERE id BETWEEN 9001 AND 9004;");
    }

    private static void execute(String sql) {
        ScriptUtils.executeSqlScript(connection, new EncodedResource(
                new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
    }

    private static long count(String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }
}
