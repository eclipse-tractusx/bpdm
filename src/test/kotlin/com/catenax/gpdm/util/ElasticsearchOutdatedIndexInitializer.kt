package com.catenax.gpdm.util

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

/**
 * When used in a Spring Boot test starts a singleton Elasticsearch container with an invalid/outdated index that should be deleted on application startup
 *
 * Used for testing the index deletion and creation logic on startup
 */
class ElasticsearchOutdatedIndexInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    companion object {
        val elasticsearchContainer = ElasticsearchContextInitializer.provideElasticsearchContainer()
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        elasticsearchContainer.start()
        ElasticsearchContextInitializer.createIndex(elasticsearchContainer, null, "{\"outdated-property\": \"property-value\"}")
        ElasticsearchContextInitializer.applyElasticsearchProperties(applicationContext, elasticsearchContainer)
    }
}