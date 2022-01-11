package com.catenax.gpdm.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "issuing_agencies")
class IssuingAgency(
    value: String,
    shortName: String?,
    number: Int?,
): BaseNamedEntity(value, shortName, number)