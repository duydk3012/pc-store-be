package com.datn.pc_store.product.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.datn.pc_store.product.enums.ProductStatus;
import org.junit.jupiter.api.Test;

class ProductStatusConverterTest {

    private final ProductStatusConverter converter = new ProductStatusConverter();

    @Test
    void convertsStatusesToAndFromDatabaseCodes() {
        assertEquals(0, converter.convertToDatabaseColumn(ProductStatus.ON_SALE));
        assertEquals(1, converter.convertToDatabaseColumn(ProductStatus.DISCONTINUED));
        assertEquals(2, converter.convertToDatabaseColumn(ProductStatus.OUT_OF_STOCK));

        assertEquals(ProductStatus.ON_SALE, converter.convertToEntityAttribute(0));
        assertEquals(ProductStatus.DISCONTINUED, converter.convertToEntityAttribute(1));
        assertEquals(ProductStatus.OUT_OF_STOCK, converter.convertToEntityAttribute(2));
    }

    @Test
    void rejectsUnknownDatabaseStatusCode() {
        assertThrows(IllegalArgumentException.class, () -> converter.convertToEntityAttribute(99));
    }
}
