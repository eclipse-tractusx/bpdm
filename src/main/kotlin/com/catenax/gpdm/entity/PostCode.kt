package com.catenax.gpdm.entity

import javax.persistence.*

@Entity
@Table(name = "post_codes")
class PostCode (
    value: String,
    shortName: String?,
    number: Int?,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: PostCodeType
        ) : BaseNamedEntity(value, shortName, number)

enum class PostCodeType(val description: String){
    CEDEX("Courrier d’Entreprise à Distribution Exceptionnelle (business mail with special delivery)"),
    LARGE_MAIL_USER("A postal code to identify large mail users, e.g., a company or authority, by postal code"),
    OTHER("Any other alternative type"),
    POST_BOX("Specifies that this postal code is used in conjunction with a postal delivery point of type post office (PO) box"),
    REGULAR("A regular postal code")
}