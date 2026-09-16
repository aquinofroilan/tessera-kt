ALTER TABLE project_tasks 
    ADD COLUMN planned_start_date DATE,
    ADD COLUMN planned_finish_date DATE,
    ADD COLUMN actual_start_date DATE,
    ADD COLUMN actual_finish_date DATE;

CREATE TABLE project_task_dependencies (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(uuid),
    predecessor_task_id UUID NOT NULL REFERENCES project_tasks(id),
    successor_task_id UUID NOT NULL REFERENCES project_tasks(id),
    dependency_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT project_task_dependencies_no_self_link CHECK (predecessor_task_id != successor_task_id)
);

CREATE INDEX idx_project_task_deps_pred ON project_task_dependencies(predecessor_task_id);
CREATE INDEX idx_project_task_deps_succ ON project_task_dependencies(successor_task_id);
CREATE INDEX idx_project_task_deps_org ON project_task_dependencies(organization_id);
