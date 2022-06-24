package com.catenax.gpdm.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "bpdm.logging")
@ConstructorBinding
data class LogConfigProperties(
    val unknownUser: String = "Anonymous",
    val userMaxLength: Int = 40
)
