package com.catenax.gpdm.entity

import javax.persistence.*

@Entity
@Table(name = "identifiers")
class Identifier(
    @Column(name = "`value`", nullable = false)
    val value: String,
    @ManyToOne
    @JoinColumn(name = "type_id", nullable = false)
    val type: IdentifierType,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: IdentifierStatus,
    @ManyToOne
    @JoinColumn(name = "issuing_body_id", nullable = false)
    val issuingBody: IssuingBody,
    @ManyToOne
    @JoinColumn(name = "partner_id", nullable = false)
    val partner: BusinessPartner
) : BaseEntity()


enum class IdentifierStatus(private val typeName: String): NamedType{
    GOLD("Gold"),
    SILVER("Silver"),
    BRONZE("Bronze"),
    UNKNOWN("Unknown");

    override fun getTypeName(): String {
        return typeName
    }
}