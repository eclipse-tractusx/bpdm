package com.catenax.gpdm.entity

import javax.persistence.*

@Entity
@Table(name = "thoroughfares")
class Thoroughfare (
    value: String,
    shortName: String?,
    number: Int?,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: ThoroughfareType,

    @ManyToOne
    @JoinColumn(name="address_id", nullable=false)
    val address: Address
        ) : BaseNamedEntity(value, shortName, number)

enum class ThoroughfareType(val description: String){
    INDUSTRIAL_ZONE("An industrial zone (industrial park, industrial estate, trading estate)"),
    OTHER("Any other alternative type"),
    RIVER("A natural flowing watercourse"),
    SQUARE("An (mostly urban) area in a locality"),
    STREET("A public thoroughfare (usually paved) in a built environment"),
}