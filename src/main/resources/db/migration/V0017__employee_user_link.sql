-- HR module: link an employee record to a login user, enabling employee
-- self-service (an authenticated user resolving to their own employee record).
-- At most one employee per user within an organization.
ALTER TABLE employees
    ADD COLUMN user_id uuid REFERENCES users(uuid) ON DELETE SET NULL;

CREATE UNIQUE INDEX employees_unique_user_per_org
    ON employees(organization_id, user_id)
    WHERE user_id IS NOT NULL;
