package com.catenax.gpdm.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "bpdm.logging")
@ConstructorBinding
data class LogConfigProperties(
    val showRequest: Boolean = true,
    val showUser: Boolean = true,
    val unknownUser: String = "Anonymous",
    val userMaxLength: Int = 40
)
