package com.catenax.gpdm.entity

import com.fasterxml.jackson.databind.ser.Serializers
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "identifier_stati")
class IdentifierStatus(
    @Column(name = "name", nullable = false)
    val name: String,
    @Column(name = "technical_key", nullable = false)
    val technicalKey: String
) : BaseEntity()