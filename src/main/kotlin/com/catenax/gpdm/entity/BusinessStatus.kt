package com.catenax.gpdm.entity

import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "business_stati")
class BusinessStatus (
    @Column(name="denotation")
    val officialDenotation: String?,
    @Column(name="valid_from", nullable = false)
    val validFrom: LocalDateTime,
    @Column(name="valid_to")
    val validUntil: LocalDateTime?,
    @Column(name="type", nullable = false)
    val type: BusinessStatusType,
    @ManyToOne
    @JoinColumn(name = "partner_id", nullable = false)
    val partner: BusinessPartner
        ): BaseEntity()

enum class BusinessStatusType(private val statusName: String, private val url: String): NamedUrlType{
    ACTIVE("Active", ""),
    DISSOLVED("Dissolved", ""),
    IN_LIQUIDATION("In Liquidation", ""),
    INACTIVE("Inactive", ""),
    INSOLVENCY("Insolvency", "");

    override fun getTypeName(): String {
        return statusName
    }

    override fun getUrl(): String {
        return url
    }
}