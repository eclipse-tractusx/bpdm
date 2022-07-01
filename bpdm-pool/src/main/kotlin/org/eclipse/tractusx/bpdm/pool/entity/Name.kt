package org.eclipse.tractusx.bpdm.pool.entity

import com.neovisionaries.i18n.LanguageCode
import javax.persistence.*

@Entity
@Table(name = "names",
    indexes = [
        Index(columnList = "partner_id")
    ])
class Name(
    @Column(name = "`value`", nullable = false)
    val value: String,
    @Column(name = "shortName")
    val shortName: String?,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: NameType,
    @Column(name = "language", nullable = false)
    @Enumerated(EnumType.STRING)
    val language: LanguageCode,
    @ManyToOne
    @JoinColumn(name="partner_id", nullable=false)
    var partner: BusinessPartner
) : BaseEntity()

enum class NameType(private val typeName: String, private val url: String): NamedUrlType, HasDefaultValue<NameType> {
    ACRONYM("An acronym commonly used for a business partner.", ""),
    DOING_BUSINESS_AS("Alternative names a company employs for doing business", ""),
    ESTABLISHMENT("Name that is used in conjunction with the registered name to name a specific organizational unit", ""),
    INTERNATIONAL("The international version of the local name of a business partner", ""),
    LOCAL("The business partner name identifies a business partner in a given context, e.g. a country or region.", ""),
    OTHER("Any other alternative name used for a company, such as a specific language variant.", ""),
    REGISTERED("The main name under which a business is officially registered in a country's business register.", ""),
    TRANSLITERATED("The transliterated name is not an officially used name, but a construct that helps to better find business partners with registered names in non-latin characters", ""),
    VAT_REGISTERED("The name which is associated with the VAT number of a business partner, i.e. the name stored in a VAT register.", "");

    override fun getTypeName(): String {
        return typeName
    }

    override fun getUrl(): String {
        return url
    }

    override fun getDefault(): NameType {
        return OTHER
    }
}