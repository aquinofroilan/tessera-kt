-- Manufacturing execution: WIP-clearing pattern.
-- On material consumption against a work order, raw-material inventory is
-- moved into Work-in-Process (1250). On completion, WIP is cleared into
-- finished-goods inventory (1200). Two new stock-movement types -- WIP_ISSUE
-- and WIP_RECEIPT -- act as the inventory side of those postings so that
-- stock-on-hand stays accurate while the GL pair reconciles to the WIP
-- balance.

ALTER TABLE stock_movements DROP CONSTRAINT stock_movements_type_chk;
ALTER TABLE stock_movements ADD CONSTRAINT stock_movements_type_chk CHECK (
    type IN ('RECEIPT','ISSUE','TRANSFER','ADJUSTMENT','OPENING_BALANCE','WIP_ISSUE','WIP_RECEIPT')
);

INSERT INTO accounts (id, code, name, type, organization_id, is_active, is_system_account)
SELECT gen_random_uuid(), '1250', 'Work-in-Process Inventory', 'ASSET', o.uuid, true, true
FROM organizations o
WHERE NOT EXISTS (
    SELECT 1 FROM accounts a WHERE a.organization_id = o.uuid AND a.code = '1250'
);

ALTER TABLE mfg_wo_components
    ADD COLUMN issued_cost numeric(18,4) NOT NULL DEFAULT 0;
ALTER TABLE mfg_work_orders
    ADD COLUMN total_issued_cost numeric(18,4) NOT NULL DEFAULT 0,
    ADD COLUMN total_completed_cost numeric(18,4) NOT NULL DEFAULT 0;
