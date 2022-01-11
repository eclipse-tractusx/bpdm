package com.catenax.gpdm.entity

import com.neovisionaries.i18n.LanguageCode
import javax.persistence.*

@Entity
@Table(name = "premises")
class Premise(
    value: String,
    shortName: String?,
    number: Int?,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: PremiseType,

    @ManyToOne
    @JoinColumn(name="address_id", nullable=false)
    val address: Address
) : BaseNamedEntity(value, shortName, number)

enum class PremiseType(val description: String){
    BUILDING("A structure (such as a house, hospital, school, etc.) with a roof and walls that is used as a place for people to live, work, do activities, store things, etc."),
    OTHER("Any other alternative type"),
    LEVEL("Level or floor in a multi-story building."),
    HARBOUR("A body of water where ships, boats and barges seek shelter from stormy weather, or are stored for future use."),
    ROOM("A distinct room in a building."),
    SUITE("A suite is the location of a business within a shopping mall or office building. The suite's number also serves as a sort of address within an address for purposes of mail delivery and pickup."),
    UNIT("Commercial units such as in shopping centers."),
    WAREHOUSE("A warehouse is a commercial building for storage of goods.")
}