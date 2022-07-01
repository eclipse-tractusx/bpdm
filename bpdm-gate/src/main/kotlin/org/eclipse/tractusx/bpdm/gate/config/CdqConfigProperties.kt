package org.eclipse.tractusx.bpdm.gate.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "bpdm.cdq")
@ConstructorBinding
class CdqConfigProperties(
    val host: String = "https://api.cdq.com",
    val api: String = "data-exchange/rest/v4",
    val storage: String = "",
    val datasource: String = "",
    val apiKey: String = "",
)