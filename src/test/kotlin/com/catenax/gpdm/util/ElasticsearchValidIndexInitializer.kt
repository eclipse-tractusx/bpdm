package com.catenax.gpdm.util

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.util.ResourceUtils
import java.nio.file.Files

/**
 * When used in a Spring Boot test starts a singleton Elasticsearch container and initializes a valid index populated with a business partner document
 */
class ElasticsearchValidIndexInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    companion object {
        val elasticsearchContainer = ElasticsearchContextInitializer.provideElasticsearchContainer()
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        elasticsearchContainer.start()

        val indexFile = ResourceUtils.getFile("classpath:elastic/index.json")
        val indexJson = Files.readString(indexFile.toPath())

        val docFile = ResourceUtils.getFile("classpath:elastic/doc-business-partner.json")
        val docJson = Files.readString(docFile.toPath())

        ElasticsearchContextInitializer.createIndex(elasticsearchContainer, indexJson, docJson)
        ElasticsearchContextInitializer.applyElasticsearchProperties(applicationContext, elasticsearchContainer)
    }
}