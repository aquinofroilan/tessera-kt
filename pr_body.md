## Summary

This release promotes accumulated features, refactors, and bug fixes from `staging` to `production`.

### 📦 Major Feature Modules Included

#### 1. Fixed Assets Module
- **Fixed Asset Foundation**: Asset tracking, categories, depreciation schedules, and status lifecycles.
- **Monthly Depreciation Runs**: Idempotent monthly depreciation run calculation, line-item audits, and automatic GL journal entry posting.
- **Asset Disposals & Write-Offs**: Disposals by sale, scrap, or write-off with net book value (NBV) calculation and gain/loss GL journal entry posting.
- **Asset Reports**: Fixed asset register snapshots and forward-looking multi-period depreciation schedules.

#### 2. Notifications System
- **In-App Notification Feed**: Per-user notification feed with unread counters and read status management.
- **Delivery Channels & Preferences**: SMTP email channel via outbox dispatcher, HTML rendering with plain-text fallback, and per-user delivery preferences.
- **Event-Driven Wiring**: Automated notifications for Leave requests and Purchase Request approvals/rejections.
- **Real-time Updates**: Server-Sent Events (SSE) stream for live unread notification count updates.

#### 3. Platform & Architectural Enhancements
- **UUID Architecture Standardization**: System-wide migration of primary keys, foreign keys, and DTOs across Finance, Notifications, and Fixed Assets domains to `java.util.UUID`.
- **Flyway Database Migrations**: Resolved migration versioning collisions up to `V0047`.
- **Code Quality**: Strict `ktlintFormat` compliance and clean test suite.

---

### 🧪 Verification
- Automated Kotlin compilation (`compileKotlin`, `compileTestKotlin`) passed.
- Linting checks (`ktlintFormat`) verified clean across all modules.
