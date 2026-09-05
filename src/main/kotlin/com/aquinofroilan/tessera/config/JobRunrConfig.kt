package com.aquinofroilan.tessera.config

import org.jobrunr.jobs.mappers.JobMapper
import org.jobrunr.storage.StorageProvider
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class JobRunrConfig {
    @Bean
    fun storageProvider(
        dataSource: DataSource,
        jobMapper: JobMapper,
    ): StorageProvider {
        val storageProvider = SqlStorageProviderFactory.using(dataSource)
        storageProvider.setJobMapper(jobMapper)
        return storageProvider
    }
}
