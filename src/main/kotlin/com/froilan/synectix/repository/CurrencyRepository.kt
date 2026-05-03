package com.froilan.synectix.repository

import com.froilan.synectix.model.Currency
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface CurrencyRepository : MongoRepository<Currency, String>
