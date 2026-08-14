package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "asset_depreciation_run_lines")
data class AssetDepreciationRunLine(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),
    @Column(name = "run_id", columnDefinition = "uuid")
    val runId: UUID,
    @Column(name = "asset_id", columnDefinition = "uuid")
    val assetId: UUID,
    @Column(name = "depreciation_amount")
    val depreciationAmount: BigDecimal,
    @Column(name = "debit_account_id", columnDefinition = "uuid")
    val debitAccountId: UUID? = null,
    @Column(name = "credit_account_id", columnDefinition = "uuid")
    val creditAccountId: UUID? = null,
)
