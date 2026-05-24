-- Hibernate binds Kotlin String fields as VARCHAR. Postgres has an assignment
-- cast from varchar to uuid (so INSERT works) but no implicit cast (so
-- `WHERE uuid_col = :string_param` fails with "operator does not exist: uuid =
-- character varying"). Upgrading the existing cast to IMPLICIT makes equality
-- comparisons work without rewriting every entity field to java.util.UUID.
DROP CAST IF EXISTS (varchar AS uuid);
CREATE CAST (varchar AS uuid) WITH INOUT AS IMPLICIT;
