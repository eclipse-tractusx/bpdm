package com.catenax.gpdm.adapter.cdq.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "bpdm.adapter.cdq.id")
@ConstructorBinding
class CdqIdentifierConfigProperties (
    val typeKey: String = "CDQID",
    val typeName: String = "CDQ Identifier",
    val statusImportedKey: String = "CDQ_IMPORTED",
    val statusImportedName: String = "Imported from CDQ but not synchronized",
    val statusSynchronizedKey: String = "CDQ_SYNCHRONIZED",
    val statusSynchronizedName: String = "Synchronized with CDQ",
    val issuerKey: String = "CDQ",
    val issuerName: String = "CDQ AG"
        )