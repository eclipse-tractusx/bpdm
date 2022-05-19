package com.catenax.gpdm.util

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.util.ResourceUtils
import java.nio.file.Files

class ElasticsearchValidIndexInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    companion object {
        val elasticsearchContainer = ElasticsearchContextInitializer.provideElasticsearchContainer()
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        elasticsearchContainer.start()
        createValidIndex()
        ElasticsearchContextInitializer.applyElasticsearchProperties(applicationContext, elasticsearchContainer)
    }

    private fun createValidIndex() {
        val indexFile = ResourceUtils.getFile("classpath:elastic/index.json")
        val indexJson = Files.readString(indexFile.toPath())

        val docFile = ResourceUtils.getFile("classpath:elastic/doc-business-partner.json")
        val docJson = Files.readString(docFile.toPath())

        elasticsearchContainer.execInContainer(
            "curl",
            "-X PUT",
            "-H", "Content-Type: application/json",
            "-d $indexJson",
            "http://localhost:9200/business-partner"
        )
        elasticsearchContainer.execInContainer(
            "curl",
            "-X PUT",
            "-H", "Content-Type: application/json",
            "-d $docJson",
            "http://localhost:9200/business-partner/_doc/BPNL000000000001"
        )
    }
}