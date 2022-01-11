package com.catenax.gpdm.entity

import com.neovisionaries.i18n.LanguageCode
import javax.persistence.*

@Entity
@Table(name = "legal_forms")
class LegalForm(
    value: String,
    shortName: String?,
    number: Int?,
    @Column(name = "type", nullable = false)
    val type: String
) : BaseNamedEntity(value, shortName, number)