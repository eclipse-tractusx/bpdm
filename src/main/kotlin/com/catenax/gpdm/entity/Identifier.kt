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
    @ManyToOne
    @JoinColumn(name = "status")
    val status: IdentifierStatus?,
    @ManyToOne
    @JoinColumn(name = "issuing_body_id")
    val issuingBody: IssuingBody?,
    @ManyToOne
    @JoinColumn(name = "partner_id", nullable = false)
    val partner: BusinessPartner
) : BaseEntity()