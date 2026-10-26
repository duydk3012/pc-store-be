-- V15__add_is_terminated_to_product_serials.sql
-- Add is_terminated column to product_serials table to indicate rejected/cancelled receipt serials

ALTER TABLE product_serials ADD COLUMN is_terminated TINYINT(1) NOT NULL DEFAULT 0;
CREATE INDEX idx_ps_is_terminated ON product_serials (is_terminated);

-- Drop unique constraint on serial_number to allow duplicate serial_numbers when is_terminated = 1
ALTER TABLE product_serials DROP INDEX uk_ps_serial_number;
CREATE INDEX idx_ps_serial_number ON product_serials (serial_number);
