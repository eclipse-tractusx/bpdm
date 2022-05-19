package com.catenax.gpdm.util

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.elasticsearch.ElasticsearchContainer

/**
 * When used on a spring boot test, starts a singleton elasticsearch container that is shared between all integration tests.
 */
open class ElasticsearchContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

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
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        elasticsearchContainer.start()
        applyElasticsearchProperties(applicationContext, elasticsearchContainer)
    }


}