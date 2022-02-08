package com.catenax.gpdm.entity

import javax.persistence.*

@Entity
@Table(name = "classifications")
class Classification (
    @Column(name = "`value`", nullable = false)
    val value: String,
    @Column(name = "code")
    val code: String?,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: ClassificationType,
    @ManyToOne
    @JoinColumn(name = "partner_id")
    val partner: BusinessPartner
        ): BaseEntity()

enum class ClassificationType(val url: String){
    NACE(""),
    NAF(""),
    NAICS( ""),
    SIC("")
}