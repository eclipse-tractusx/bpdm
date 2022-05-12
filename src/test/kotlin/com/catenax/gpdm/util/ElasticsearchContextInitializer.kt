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
        private const val memoryInBytes = 1024L * 1024L * 1024L  //1 gb
        private const val memorySwapInBytes = 4L * 1024L * 1024L * 1024L * 1024L //4 gb

        val elasticsearchContainer = ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.0")
            .withEnv("discovery.type", "single-node")
            .withCreateContainerCmdModifier { cmd ->
                cmd.hostConfig!!
                    .withMemory(memoryInBytes)
                    .withMemorySwap(memorySwapInBytes)
            }!!
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        elasticsearchContainer.start()
        TestPropertyValues.of(
            "spring.elasticsearch.uris=${elasticsearchContainer.httpHostAddress}",
            "bpdm.elastic.enabled=true"
        ).applyTo(applicationContext.environment)
    }
}