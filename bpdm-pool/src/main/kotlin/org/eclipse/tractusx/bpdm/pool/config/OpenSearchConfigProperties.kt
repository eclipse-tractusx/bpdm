package org.eclipse.tractusx.bpdm.pool.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "bpdm.opensearch")
@ConstructorBinding
class OpenSearchConfigProperties(
    val enabled: Boolean = false,
    val host: String = "localhost",
    val port: Int = 9200,
    val scheme: String = "http",
    val exportPageSize: Int = 100,
    val refreshOnWrite: Boolean = false
)