-- Phase 2: partial receipts/fulfillment + 3-way match quantity tracking.

ALTER TABLE purchase_order_lines
    ADD COLUMN received_quantity numeric(18,4) NOT NULL DEFAULT 0,
    ADD COLUMN billed_quantity   numeric(18,4) NOT NULL DEFAULT 0;

ALTER TABLE sales_order_lines
    ADD COLUMN fulfilled_quantity numeric(18,4) NOT NULL DEFAULT 0,
    ADD COLUMN invoiced_quantity  numeric(18,4) NOT NULL DEFAULT 0;

ALTER TABLE purchase_orders DROP CONSTRAINT purchase_orders_status_chk;
ALTER TABLE purchase_orders
    ADD CONSTRAINT purchase_orders_status_chk
        CHECK (status IN ('DRAFT','APPROVED','PARTIALLY_RECEIVED','RECEIVED','CLOSED','CANCELLED'));

ALTER TABLE sales_orders DROP CONSTRAINT sales_orders_status_chk;
ALTER TABLE sales_orders
    ADD CONSTRAINT sales_orders_status_chk
        CHECK (status IN ('DRAFT','APPROVED','PARTIALLY_FULFILLED','FULFILLED','CLOSED','CANCELLED'));
