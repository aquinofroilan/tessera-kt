package com.froilan.synectix.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "currencies")
data class Currency(
    @Id
    @Column(columnDefinition = "char(3)")
    val code: String,
    val name: String,
    val symbol: String,
    @Column(name = "decimal_places")
    val decimalPlaces: Int,
)
