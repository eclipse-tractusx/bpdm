package com.catenax.gpdm.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "roles")
class Role (
    @Column(name = "technical_key", unique = true, nullable = false)
    val technicalKey: String,
    @Column(name = "name", nullable = false)
    val name: String
        ): BaseEntity()