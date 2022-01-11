package com.catenax.gpdm.entity

import javax.persistence.*

@Entity
@Table(name = "administrative_area_codes")
class AdministrativeAreaCode (
    @Column(name = "`value`", nullable = false)
    val value: String,
    @Column(name = "type", nullable = false)
    val type: String,
    @ManyToOne
    @JoinColumn(name = "area_id", nullable = false)
    val area: AdministrativeArea
        ) : BaseEntity()