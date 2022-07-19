package org.eclipse.tractusx.bpdm.pool.entity

import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.model.HasDefaultValue
import org.eclipse.tractusx.bpdm.common.model.NamedUrlType
import javax.persistence.*

@Entity
@Table(name = "postal_delivery_points",
    indexes = [
        Index(columnList = "address_id")
    ])
class PostalDeliveryPoint(
    @Column(name = "`value`", nullable = false)
    val value: String,
    @Column(name = "short_name")
    val shortName: String?,
    @Column(name = "number")
    val number: String?,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: PostalDeliveryPointType,
    @Column(name = "language", nullable = false)
    @Enumerated(EnumType.STRING)
    val language: LanguageCode,
    @ManyToOne
    @JoinColumn(name="address_id", nullable=false)
    var address: Address
) : BaseEntity()

enum class PostalDeliveryPointType(private val typeName: String, private val url: String): NamedUrlType, HasDefaultValue<PostalDeliveryPointType> {
    INTERURBAN_DELIVERY_POINT("Interurban Delivery Point", ""),
    MAIL_STATION("Mail Station", ""),
    MAILBOX("Mailbox", ""),
    OTHER("Other Type", ""),
    POST_OFFICE_BOX("Post Office Box", "");

    override fun getTypeName(): String {
        return typeName
    }

    override fun getUrl(): String {
        return url
    }

    override fun getDefault(): PostalDeliveryPointType {
        return OTHER
    }
}