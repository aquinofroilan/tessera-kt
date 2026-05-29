-- Stock movement reversal: a movement can be reversed by a compensating
-- movement, which lets order cancellation unwind received/fulfilled stock.
ALTER TABLE stock_movements
    ADD COLUMN reversed boolean NOT NULL DEFAULT false,
    ADD COLUMN reversal_of_movement_id uuid REFERENCES stock_movements(id) ON DELETE SET NULL;

CREATE INDEX stock_movements_org_reference_idx ON stock_movements(organization_id, reference);
