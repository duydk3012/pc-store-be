-- Keep V1-V12 unchanged. Existing products without a current price remain NULL.
ALTER TABLE products ADD COLUMN price BIGINT NULL AFTER code;

-- Deterministic backfill from the most recent price that is effective now.
UPDATE products p
SET p.price = (
    SELECT pp.price
    FROM product_price pp
    WHERE pp.product_id = p.id
      AND pp.is_present_price = 1
      AND (pp.effective_from IS NULL OR pp.effective_from <= UTC_TIMESTAMP(6))
      AND (pp.effective_to IS NULL OR pp.effective_to > UTC_TIMESTAMP(6))
    ORDER BY COALESCE(pp.effective_from, pp.created_date) DESC, pp.id DESC
    LIMIT 1
);
