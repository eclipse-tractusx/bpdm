package com.catenax.gpdm.util

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

class ElasticsearchOutdatedIndexInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    companion object {
        val elasticsearchContainer = ElasticsearchContextInitializer.provideElasticsearchContainer()
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        elasticsearchContainer.start()
        createOutdatedIndex()
        ElasticsearchContextInitializer.applyElasticsearchProperties(applicationContext, elasticsearchContainer)
    }

    private fun createOutdatedIndex() {
        elasticsearchContainer.execInContainer("curl", "-X PUT", "http://localhost:9200/business-partner")
        elasticsearchContainer.execInContainer(
            "curl",
            "-X POST",
            "-H", "Content-Type: application/json",
            "-d {\"outdated-property\": \"property-value\"}",
            "http://localhost:9200/business-partner/_doc"
        )
    }
}