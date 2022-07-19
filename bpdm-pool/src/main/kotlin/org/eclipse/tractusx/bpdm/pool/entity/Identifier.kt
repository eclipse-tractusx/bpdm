package org.eclipse.tractusx.bpdm.pool.entity

import javax.persistence.*

@Entity
@Table(name = "identifiers",
    indexes = [
        Index(columnList = "partner_id"),
        Index(columnList = "type_id"),
        Index(columnList = "status"),
        Index(columnList = "issuing_body_id")
    ])
class Identifier(
    @Column(name = "`value`", nullable = false)
    var value: String,
    @ManyToOne
    @JoinColumn(name = "type_id", nullable = false)
    var type: IdentifierType,
    @ManyToOne
    @JoinColumn(name = "status")
    var status: IdentifierStatus?,
    @ManyToOne
    @JoinColumn(name = "issuing_body_id")
    var issuingBody: IssuingBody?,
    @ManyToOne
    @JoinColumn(name = "partner_id", nullable = false)
    var partner: BusinessPartner
) : BaseEntity()