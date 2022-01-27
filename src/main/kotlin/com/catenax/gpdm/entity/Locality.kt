package com.catenax.gpdm.entity

import com.neovisionaries.i18n.LanguageCode
import javax.persistence.*

@Entity
@Table(name = "localities")
class Locality (
    value: String,
    shortName: String?,
    number: Int?,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val localityType: LocalityType,
    @ManyToOne
    @JoinColumn(name = "address_id", nullable = false)
    val address: Address
        ) : BaseNamedEntity(value, shortName, number)

enum class LocalityType(val description: String){
    BLOCK("The smallest area in a locality that is surrounded by streets"),
    CITY("A permanent human settlement"),
    DISTRICT("Subdivision or own part of a city, or smaller settlements as part of a larger commune"),
    OTHER("Any other alternative type"),
    POST_OFFICE_CITY("The city of the post office of a certain recipient"),
    QUARTER("A named section of a locality")
}