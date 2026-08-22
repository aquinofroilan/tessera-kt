package com.aquinofroilan.tessera.domain.finance.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "currencies")
class Currency(
    @Id
    @Column(columnDefinition = "char(3)")
    var code: String,
    var name: String,
    var symbol: String,
    @Column(name = "decimal_places")
    var decimalPlaces: Int,
)
