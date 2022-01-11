package com.catenax.gpdm.entity

import javax.persistence.Entity
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "care_ofs")
class CareOf (
    value: String,
    shortName: String?,
    number: Int?,
    @OneToOne(mappedBy = "careOf")
    val address: Address
        ): BaseNamedEntity(value, shortName, number)