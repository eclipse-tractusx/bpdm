package com.catenax.gpdm.entity

import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "care_ofs")
class CareOf (
    value: String,
    shortName: String?,
    number: Int?
        ): BaseNamedEntity(value, shortName, number)