package com.catenax.gpdm.entity

import com.neovisionaries.i18n.LanguageCode
import javax.persistence.*

@Entity
@Table(name = "postal_delivery_points")
class PostalDeliveryPoint(
    value: String,
    shortName: String?,
    number: Int?,
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    val type: PostalDeliveryPointType,

    @ManyToOne
    @JoinColumn(name="address_id", nullable=false)
    val address: Address
) : BaseNamedEntity(value, shortName, number)

enum class PostalDeliveryPointType(val description: String){
    INTERURBAN_DELIVERY_POINT("A delivery point which is specified by a kilometre information along a road. In most cases there exist no house number for such locations."),
    MAIL_STATION("A cluster box unit where the post is delivered to."),
    MAILBOX("A location at an address where the post is delivered to."),
    OTHER("Any other alternative type."),
    POST_OFFICE_BOX("A uniquely addressable lockable box located on the premises of a post office station.")
}