package com.catenax.gpdm.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "bpdm.bpn")
@ConstructorBinding
data class BpnConfigProperties(
    val agencyName: String = "Catena-X",
    val agencyKey: String = "CATENAX",
    var name: String = "Business Partner Number",
    val id: String = "BPN",
    val legalEntityChar: Char = 'L',
    val siteChar: Char = 'S',
    val addressChar: Char = 'A',
    val counterKey: String = "bpn-counter",
    val counterKeySites: String = "bpn-s-counter",
    val counterKeyAddresses: String = "bpn-a-counter",
    val counterDigits: Int = 10,
    val checksumModulus: Int = 1271,
    val checksumRadix: Int = 36,
    val alphabet: String = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ",
    /**
     * Maximum number of bpn to identifier mappings that can be retrieved per request via the bpn search endpoint.
     */
    val searchRequestLimit: Int = 5000
)