package com.catenax.gpdm.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "bpdm.elastic")
@ConstructorBinding
data class ElasticSearchConfigProperties(
    val enabled: Boolean = false,
    val host: String = "localhost",
    val exportTimeKey: String = "elastic-last-export",
    val exportPageSize: Int = 100
)
