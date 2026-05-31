package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Currency
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CurrencyRepository : JpaRepository<Currency, String>
