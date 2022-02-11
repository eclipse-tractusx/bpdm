package com.catenax.gpdm.entity

import com.neovisionaries.i18n.LanguageCode
import javax.persistence.*

@Entity
@Table(name = "localities")
class Locality (
    @Column(name = "`value`", nullable = false)
    val value: String,
    @Column(name = "short_name")
    val shortName: String?,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val localityType: LocalityType,
    @Column(name = "language", nullable = false)
    @Enumerated(EnumType.STRING)
    val language: LanguageCode,
    @ManyToOne
    @JoinColumn(name = "address_id", nullable = false)
    val address: Address
        ) : BaseEntity()

enum class LocalityType(private val typeName: String, private val url: String): NamedUrlType, HasDefaultValue<LocalityType>{
    BLOCK("Block", ""),
    CITY("City", ""),
    DISTRICT("District", ""),
    OTHER("Other", ""),
    POST_OFFICE_CITY("Post Office City", ""),
    QUARTER("Quarter", "");

    override fun getTypeName(): String {
        return typeName
    }

    override fun getUrl(): String {
        return url
    }

    override fun getDefault(): LocalityType {
        return OTHER
    }
}