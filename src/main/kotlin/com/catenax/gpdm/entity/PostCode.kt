package com.catenax.gpdm.entity

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.neovisionaries.i18n.CountryCode
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "post_codes")
class PostCode (
    value: String,
    shortName: String?,
    number: Int?,
    @Column(name = "uuid", nullable = false, length=36, columnDefinition = "UUID")
    val uuid: UUID,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: PostCodeType,
    @Column(name = "country", nullable = false)
    @Enumerated(EnumType.STRING)
    val countryCode: CountryCode
        ) : BaseNamedEntity(value, shortName, number)

enum class PostCodeType(val description: String){
    CEDEX("Courrier d’Entreprise à Distribution Exceptionnelle (business mail with special delivery)"),
    LARGE_MAIL_USER("A postal code to identify large mail users, e.g., a company or authority, by postal code"),
    OTHER("Any other alternative type"),
    POST_BOX("Specifies that this postal code is used in conjunction with a postal delivery point of type post office (PO) box"),
    REGULAR("A regular postal code")
}