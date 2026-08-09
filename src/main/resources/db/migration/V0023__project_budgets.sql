-- Project module: budgets per project, by cost category.
-- Actuals are computed (not stored): labor actuals come from approved time
-- entries; material/expense actuals arrive once bills/expense claims carry a
-- project reference (Epic #166).
CREATE TABLE project_budgets (
    id               uuid PRIMARY KEY,
    project_id       uuid NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    category         text NOT NULL,
    budget_amount    numeric(18,4) NOT NULL,
    currency         char(3),
    organization_id  uuid NOT NULL REFERENCES organizations(uuid),
    created_at       timestamp NOT NULL DEFAULT current_timestamp,
    updated_at       timestamp,
    CONSTRAINT project_budgets_category_chk
        CHECK (category IN ('LABOR','MATERIAL','EXPENSE','OTHER')),
    CONSTRAINT project_budgets_unique_category_per_project UNIQUE (project_id, category)
);
CREATE INDEX project_budgets_org_project_idx ON project_budgets(organization_id, project_id);
