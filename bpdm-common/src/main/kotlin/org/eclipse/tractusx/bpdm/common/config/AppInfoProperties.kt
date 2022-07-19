package org.eclipse.tractusx.bpdm.common.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "bpdm")
@ConstructorBinding
data class AppInfoProperties(
    val name: String = "BPDM",
    val description: String = "",
    val version: String = ""
)
