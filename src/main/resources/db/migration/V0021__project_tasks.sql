-- Project module: tasks / work breakdown structure.
-- Tasks nest within a project via an optional self-referencing parent.
CREATE TABLE project_tasks (
    id                   uuid PRIMARY KEY,
    project_id           uuid NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    parent_task_id       uuid REFERENCES project_tasks(id) ON DELETE CASCADE,
    name                 text NOT NULL,
    description          text,
    assignee_employee_id uuid REFERENCES employees(id) ON DELETE SET NULL,
    estimated_hours      numeric(10,2),
    status               text NOT NULL DEFAULT 'TODO',
    organization_id      uuid NOT NULL REFERENCES organizations(uuid),
    created_at           timestamp NOT NULL DEFAULT current_timestamp,
    updated_at           timestamp,
    CONSTRAINT project_tasks_status_chk
        CHECK (status IN ('TODO','IN_PROGRESS','DONE','CANCELLED'))
);
CREATE INDEX project_tasks_org_project_idx ON project_tasks(organization_id, project_id);
CREATE INDEX project_tasks_parent_idx ON project_tasks(parent_task_id);
