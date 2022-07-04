package org.eclipse.tractusx.bpdm.pool.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "bpdm.datasource")
@ConstructorBinding
data class DatasourceConfigProperties(
    val host: String = "localhost"
)
