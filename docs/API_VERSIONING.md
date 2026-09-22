# Tessera ERP: API Versioning & Deprecation Policy

Tessera employs a strict versioning strategy to ensure stability and predictability for all API consumers, especially custom third-party integrations and internal mobile clients.

## 1. Version Format
All public-facing API endpoints must be explicitly versioned in their URI paths. 
**Format**: `/api/v{MAJOR_VERSION}/...` (e.g., `/api/v1/finance/ap/bills`).

- **Infrastructure Endpoints**: Operational endpoints like `/health`, `/actuator`, and `/graphql` (if schema evolution handles backward compatibility internally) are intentionally left unversioned.

## 2. Backward-Compatible Changes (Minor/Patch)
We strive to evolve the API seamlessly. The following changes are considered backward-compatible and will **not** result in a new major version:
* Adding new API endpoints.
* Adding new, optional request parameters or JSON body fields.
* Adding new fields to response payloads.
* Changing the order of properties in JSON responses.
* Adding new valid values to an `enum` (clients must be prepared to handle unknown enum values gracefully).

## 3. Breaking Changes (Major Version Bump)
A new major version (e.g., `/api/v2`) will only be introduced when breaking changes are unavoidable. A breaking change includes:
* Removing or renaming a request parameter, JSON body field, or response field.
* Changing the data type of an existing field (e.g., from `Integer` to `String`).
* Removing an existing enum value.
* Requiring a previously optional parameter or header.
* Changing the fundamental business logic or side effects of an endpoint in a way that breaks expected behavior.

## 4. Deprecation Policy
If an endpoint or a specific field is scheduled for removal or replacement:
1. **Notice Period**: A minimum of **6 months** notice will be provided before the removal of the API component.
2. **Communication**:
   * The endpoint must be annotated with `@Deprecated` in the code, which will automatically reflect in the OpenAPI/Swagger documentation.
   * Responses from deprecated endpoints will include the standard HTTP `Sunset` header indicating the exact date the endpoint will become unresponsive.
   * Deprecation announcements will be published in the release notes.
