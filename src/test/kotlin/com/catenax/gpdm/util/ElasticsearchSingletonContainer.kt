package com.catenax.gpdm.util

import org.testcontainers.elasticsearch.ElasticsearchContainer

object ElasticsearchSingletonContainer {

    private const val memoryInBytes = 1024L * 1024L * 1024L  //1 gb
    private const val memorySwapInBytes = 4L * 1024L * 1024L * 1024L * 1024L //4 gb

    val instance by lazy { start() }

    private fun start() = ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.0")
        .apply {
            withEnv("discovery.type", "single-node")
            withCreateContainerCmdModifier { cmd ->
                cmd.hostConfig!!
                    .withMemory(memoryInBytes)
                    .withMemorySwap(memorySwapInBytes)
            }
            start()
        }
}