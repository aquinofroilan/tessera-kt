-- Vendor evaluations and scorecards
CREATE TABLE IF NOT EXISTS vendor_evaluations (
    id uuid PRIMARY KEY,
    vendor_id uuid NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    organization_id uuid NOT NULL REFERENCES organizations(uuid) ON DELETE CASCADE,
    purchase_order_id uuid REFERENCES purchase_orders(id) ON DELETE SET NULL,
    evaluation_date date NOT NULL,
    delivery_score numeric(5, 2) NOT NULL,
    quality_score numeric(5, 2) NOT NULL,
    price_accuracy_score numeric(5, 2) NOT NULL,
    overall_score numeric(5, 2) NOT NULL,
    comments text,
    evaluated_by uuid NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_vendor_evaluations_vendor ON vendor_evaluations(vendor_id, evaluation_date DESC);
CREATE INDEX IF NOT EXISTS idx_vendor_evaluations_org ON vendor_evaluations(organization_id);
