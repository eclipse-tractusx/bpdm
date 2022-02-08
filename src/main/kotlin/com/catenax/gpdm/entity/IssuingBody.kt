package com.catenax.gpdm.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "issuing_bodies")
class IssuingBody (
    @Column(name = "name", nullable = false)
    val name: String,
    @Column(name = "url")
    val url: String?,
    @Column(name = "technical_key", nullable = false)
    val technicalKey: String
        ): BaseEntity()