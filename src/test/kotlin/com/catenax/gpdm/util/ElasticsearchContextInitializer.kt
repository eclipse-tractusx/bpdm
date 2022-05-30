package com.catenax.gpdm.util

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.elasticsearch.ElasticsearchContainer

/**
 * When used on a spring boot test, starts a singleton elasticsearch container that is shared between all integration tests.
 */
class ElasticsearchContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    companion object {
        private const val memoryInBytes = 500L * 1024L * 1024L  //500 mb
        private const val memorySwapInBytes = 1L * 1024L * 1024L * 1024L //1 gb

        val elasticsearchContainer = provideElasticsearchContainer()

        fun provideElasticsearchContainer(): ElasticsearchContainer {
            return ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.0")
                .withEnv("discovery.type", "single-node")
                .withCreateContainerCmdModifier { cmd ->
                    cmd.hostConfig!!
                        .withMemory(memoryInBytes)
                        .withMemorySwap(memorySwapInBytes)
                }!!
        }


        fun applyElasticsearchProperties(applicationContext: ConfigurableApplicationContext, container: ElasticsearchContainer) {
            TestPropertyValues.of(
                "spring.elasticsearch.uris=${container.httpHostAddress}",
                "bpdm.elastic.enabled=true"
            ).applyTo(applicationContext.environment)
        }

        fun createIndex(container: ElasticsearchContainer, indexPayload: String?, contentPayload: String?) {
            val curlCommand = "curl"
            val putCommand = "-X PUT"
            val contentCommands = listOf("-H", "Content-Type: application/json")
            val indexAddress = "http://localhost:9200/business-partner"

            val createIndexCommands = mutableListOf(curlCommand, putCommand, indexAddress)
            if (indexPayload != null)
                createIndexCommands.addAll(2, contentCommands.plus("-d $indexPayload"))

            container.execInContainer(*createIndexCommands.toTypedArray())

            val createContentCommands = mutableListOf(curlCommand, putCommand, "$indexAddress/_doc/BPNL000000000001")
            if (contentPayload != null)
                createContentCommands.addAll(2, contentCommands.plus("-d $contentPayload"))

            container.execInContainer(*createContentCommands.toTypedArray())
        }
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        elasticsearchContainer.start()
        applyElasticsearchProperties(applicationContext, elasticsearchContainer)
    }


}