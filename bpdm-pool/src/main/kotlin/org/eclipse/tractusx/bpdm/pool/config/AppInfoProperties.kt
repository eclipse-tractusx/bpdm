package org.eclipse.tractusx.bpdm.pool.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "bpdm")
@ConstructorBinding
data class AppInfoProperties(
    val name: String = "BPDM",
    val description: String = "Service that manages and shares business partner data with other CatenaX services",
    val version: String = "0.0.1"
)
