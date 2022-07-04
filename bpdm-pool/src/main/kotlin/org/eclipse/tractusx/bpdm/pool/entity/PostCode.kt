package org.eclipse.tractusx.bpdm.pool.entity

import com.neovisionaries.i18n.CountryCode
import javax.persistence.*

@Entity
@Table(name = "post_codes",
    indexes = [
        Index(columnList = "address_id")
    ])
class PostCode (
    @Column(name = "`value`", nullable = false)
    val value: String,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: PostCodeType,
    @Column(name = "country", nullable = false)
    @Enumerated(EnumType.STRING)
    val countryCode: CountryCode,
    @ManyToOne
    @JoinColumn(name = "address_id", nullable = false)
    var address: Address
        ) : BaseEntity()

enum class PostCodeType(private val codeName: String, private val url: String): NamedUrlType, HasDefaultValue<PostCodeType> {
    CEDEX("Courrier d’Entreprise à Distribution Exceptionnelle", ""),
    LARGE_MAIL_USER("Large mail user", ""),
    OTHER("Other type", ""),
    POST_BOX("Post Box", ""),
    REGULAR("Regular", "");

    override fun getTypeName(): String {
        return codeName
    }

    override fun getUrl(): String {
        return url
    }

    override fun getDefault(): PostCodeType {
        return OTHER
    }
}