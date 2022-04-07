package com.catenax.gpdm.util

import org.testcontainers.elasticsearch.ElasticsearchContainer

object ElasticsearchContainer {

    val instance by lazy { start() }

    private fun start() = ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.0")
        .apply {
            withEnv("discovery.type", "single-node")
            start()
        }
}