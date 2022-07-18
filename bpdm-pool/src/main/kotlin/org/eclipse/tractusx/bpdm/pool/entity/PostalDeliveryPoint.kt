package org.eclipse.tractusx.bpdm.pool.entity

import com.neovisionaries.i18n.LanguageCode
import org.eclipse.tractusx.bpdm.common.model.PostalDeliveryPointType
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

