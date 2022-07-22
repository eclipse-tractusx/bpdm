package org.eclipse.tractusx.bpdm.pool.component.opensearch.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "bpdm.opensearch")
@ConstructorBinding
class OpenSearchConfigProperties(
    val host: String = "localhost",
    val port: Int = 9201,
    val scheme: String = "http"
)