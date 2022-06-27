package com.catenax.gpdm.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "identifier_status")
class IdentifierStatus (
    @Column(name = "name", nullable = false)
    val name: String,
    @Column(name = "technicalKey", nullable = false, unique = true)
    val technicalKey: String
        ): BaseEntity()