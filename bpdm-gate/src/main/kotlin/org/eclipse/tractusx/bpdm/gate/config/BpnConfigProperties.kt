package org.eclipse.tractusx.bpdm.gate.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "bpdm.bpn")
@ConstructorBinding
data class BpnConfigProperties(
    val agencyName: String = "Catena-X",
    val agencyKey: String = "CATENAX",
    var name: String = "Business Partner Number",
    val id: String = "BPN"
)