package com.catenax.gpdm.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "bpdm.bpn")
@ConstructorBinding
data class BpnConfigProperties (
    val agencyName: String = "Catena-X",
    val agencyKey: String = "CATENAX",
    var name: String = "Business Partner Number",
    val prefix: String = "BPN",
    val legalEntityChar: Char = 'L',
    val counterKey: String = "bpn-counter",
    val counterDigits: Int = 10,
    val checksumModulus: Int = 1271,
    val checksumRadix: Int = 36,
    val alphabet: String =  "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        )