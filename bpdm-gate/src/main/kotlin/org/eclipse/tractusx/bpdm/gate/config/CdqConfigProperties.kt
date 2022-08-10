package org.eclipse.tractusx.bpdm.gate.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "bpdm.cdq")
@ConstructorBinding
class CdqConfigProperties(
    val host: String = "https://api.cdq.com",
    val storage: String = "8888865cc59a3b4aa079b8e00313cf53",
    val datasource: String = "61c096613b4b824755a62641",
    val apiKey: String = "",
    val dataExchangeApiUrl: String = "/data-exchange/rest/v4/storages/${storage}",
    val dataClinicApiUrl: String = "/data-clinic/rest/storages/${storage}"
)