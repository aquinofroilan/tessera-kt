## Issue #209: Quality Holds / Quarantine

This PR introduces the ability to place specific stock quantities, lot numbers, or serial numbers on hold (quarantine). 
Held stock is excluded from the "available-to-issue" quantity to prevent accidental fulfillment.

### Changes
* **Database**: Adds `V0083__inventory_quality_holds.sql` to add a `held_quantity` column to `stock_on_hand` and a new `inventory_holds` audit table.
* **Entities**: Adds `InventoryHold` entity and updates `SerialStatus` to include a `QUARANTINED` state.
* **Queries**: Updates `StockOnHandQueries`'s `applyDelta` to evaluate `quantity - held_quantity >= delta`, and adds `applyHold` to increase/decrease the held reservation.
* **Services**: Adds `InventoryHoldService` with `placeOnHold` and `releaseHold` logic (validating availability and statuses).
* **API**: Exposes endpoints in `InventoryHoldController` (`POST /api/v1/inventory/holds`, `POST .../release`, `GET .../holds`).

Closes #209
