package com.froilan.synectix.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "currencies")
data class Currency(
    @Id
    val code: String,
    val name: String,
    val symbol: String,
    val decimalPlaces: Int,
)
