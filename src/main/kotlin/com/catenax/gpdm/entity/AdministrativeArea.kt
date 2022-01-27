package com.catenax.gpdm.entity

import com.neovisionaries.i18n.CountryCode
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "administrative_areas")
class AdministrativeArea(
    @Column(name = "uuid", nullable = false, length=36, columnDefinition = "BINARY(36)")
    val uuid: UUID,
    @Column(name = "`value`", nullable = false)
    val value: String,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: AdministrativeAreaType,
    @Column(name = "country", nullable = false)
    @Enumerated(EnumType.STRING)
    val countryCode: CountryCode
): BaseEntity(){
    @OneToMany(mappedBy = "area")
    lateinit var codes: Set<AdministrativeAreaCode>
}

enum class AdministrativeAreaType(val description: String){
    COUNTY("Level 2 subdivision of a country, subdivision of a region."),
    REGION("Top level subdivision of a country."),
    OTHER("Any other alternative type.")
}