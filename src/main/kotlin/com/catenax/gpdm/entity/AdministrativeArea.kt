package com.catenax.gpdm.entity

import javax.persistence.*

@Entity
@Table(name = "administrative_areas")
class AdministrativeArea(
    @Column(name = "`value`", nullable = false)
    val name: String,
    @OneToMany(mappedBy = "area")
    val codes: Set<AdministrativeAreaCode>,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: AdministrativeAreaType
): BaseEntity()

enum class AdministrativeAreaType(val description: String){
    COUNTY("Level 2 subdivision of a country, subdivision of a region."),
    REGION("Top level subdivision of a country."),
    OTHER("Any other alternative type.")
}