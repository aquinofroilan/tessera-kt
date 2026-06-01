-- HR module: department org-chart hierarchy.
-- Adds an optional self-referencing parent so departments can nest into an org chart.
ALTER TABLE departments
    ADD COLUMN parent_id uuid REFERENCES departments(id);

CREATE INDEX departments_parent_idx ON departments(parent_id);
