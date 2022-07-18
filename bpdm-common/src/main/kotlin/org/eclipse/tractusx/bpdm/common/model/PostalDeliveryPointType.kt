package org.eclipse.tractusx.bpdm.common.model

enum class PostalDeliveryPointType(private val typeName: String, private val url: String) : NamedUrlType, HasDefaultValue<PostalDeliveryPointType> {
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