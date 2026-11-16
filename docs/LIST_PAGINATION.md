# Optional pagination for inventory and tax lists

The following list routes accept optional `page` and `size` query parameters:

| Route | Existing filters |
|---|---|
| `GET /api/v1/inventory/products` | `categoryId`, `isActive` (defaults to `true`), `search` |
| `GET /api/v1/inventory/products/{productId}/variants` | None |
| `GET /api/v1/inventory/variants` | `productId`, `isActive` |
| `GET /api/v1/inventory/categories` | `activeOnly` (defaults to `true`) |
| `GET /api/v1/finance/tax/rates` | `active` (omitted means all rates) |
| `GET /api/v1/finance/tax/groups` | `active` (omitted means all groups) |

Pagination is opt-in to preserve existing clients. If both `page` and `size` are omitted, the route returns the existing JSON array response. If either is supplied, the response is an object with a page of results and metadata:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

`page` is zero-based and defaults to `0` when pagination is requested. `size` defaults to `20` and must be between `1` and `100`. Negative page numbers, out-of-range sizes, and offsets too large for the database paging API return HTTP `400 Bad Request`. Pagination can be combined with the existing filters in the table.

Examples:

```http
GET /api/v1/inventory/products?page=0&size=20&isActive=true
GET /api/v1/inventory/variants?productId=01a19ac2-b3ca-75a8-b946-eedd7b548ebc&page=1&size=10
GET /api/v1/finance/tax/groups?page=0
```
