-- V17__change_product_image_url_to_longtext.sql
-- Change image_url column in products table from VARCHAR(500) to LONGTEXT to support longer URLs / Base64 images
ALTER TABLE products MODIFY COLUMN image_url LONGTEXT NULL;
