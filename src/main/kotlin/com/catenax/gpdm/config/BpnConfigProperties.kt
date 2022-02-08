package com.catenax.gpdm.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "bpdm.bpn")
data class BpnConfigProperties (
    var agencyName: String = "Catena-X",
    var agencyKey: String = "CATENAX",
    var name: String = "Business Partner Number",
    var prefix: String = "BPN",
    var legalEntityChar: Char = 'L',
    var counterKey: String = "bpn-counter",
    var counterDigits: Int = 10,
    var checksumModulus: Int = 1271,
    var checksumRadix: Int = 36,
    var alphabet: String =  "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        )