package com.catenax.gpdm.entity

import javax.persistence.*

@Entity
@Table(name = "classifications")
class Classification (
    value: String,
    shortName: String?,
    number: Int?,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: ClassificationType
        ): BaseNamedEntity(value, shortName, number)

enum class ClassificationType(val description: String){
    NACE("Industry standard classification system used in the European Union."),
    NAF("French classification of economic activities. "),
    NAICS("Classification of business establishments by type of economic activity. It has largely replaced SIC system."),
    SIC("System for classifying industries by a four-digit code.")
}