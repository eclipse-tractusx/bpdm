package org.eclipse.tractusx.bpdm.pool.entity

import org.eclipse.tractusx.bpdm.common.model.BusinessStatusType
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "business_stati",
    indexes = [
        Index(columnList = "partner_id")
    ])
class BusinessStatus (
    @Column(name="denotation", nullable = false)
    val officialDenotation: String,
    @Column(name="valid_from", nullable = false)
    val validFrom: LocalDateTime,
    @Column(name="valid_to")
    val validUntil: LocalDateTime?,
    @Column(name="type", nullable = false)
    val type: BusinessStatusType,
    @ManyToOne
    @JoinColumn(name = "partner_id", nullable = false)
    var partner: BusinessPartner
        ): BaseEntity()