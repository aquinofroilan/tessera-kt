CREATE TABLE kpi_definitions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(uuid),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    metric_type VARCHAR(50) NOT NULL,
    target_value DECIMAL(19, 4) NOT NULL,
    warning_threshold DECIMAL(19, 4),
    critical_threshold DECIMAL(19, 4),
    is_higher_better BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE kpi_values (
    id UUID PRIMARY KEY,
    kpi_id UUID NOT NULL REFERENCES kpi_definitions(id) ON DELETE CASCADE,
    period_date DATE NOT NULL,
    actual_value DECIMAL(19, 4) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    UNIQUE(kpi_id, period_date)
);
