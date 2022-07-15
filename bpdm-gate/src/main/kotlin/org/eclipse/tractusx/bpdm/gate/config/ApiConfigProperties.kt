package org.eclipse.tractusx.bpdm.gate.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "bpdm.api")
@ConstructorBinding
data class ApiConfigProperties(
    val upsertLimit: Int = 100,
)