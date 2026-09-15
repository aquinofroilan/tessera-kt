CREATE TABLE shifts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(uuid),
    name VARCHAR(255) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rosters (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(uuid),
    name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roster_entries (
    id UUID PRIMARY KEY,
    roster_id UUID NOT NULL REFERENCES rosters(id),
    employee_id UUID NOT NULL REFERENCES employees(id),
    shift_id UUID NOT NULL REFERENCES shifts(id),
    shift_date DATE NOT NULL
);

ALTER TABLE attendance_records ADD COLUMN planned_shift_id UUID REFERENCES shifts(id);

CREATE INDEX idx_shifts_org ON shifts(organization_id);
CREATE INDEX idx_rosters_org ON rosters(organization_id);
CREATE INDEX idx_roster_entries_roster ON roster_entries(roster_id);
CREATE INDEX idx_roster_entries_employee ON roster_entries(employee_id);
CREATE INDEX idx_roster_entries_date ON roster_entries(shift_date);
