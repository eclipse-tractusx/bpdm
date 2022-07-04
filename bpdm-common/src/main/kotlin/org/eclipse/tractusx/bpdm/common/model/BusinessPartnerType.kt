package org.eclipse.tractusx.bpdm.common.model

enum class BusinessPartnerType(private val typeName: String, private val url: String) : NamedUrlType, HasDefaultValue<BusinessPartnerType> {
    BRAND("Brand", ""),
    LEGAL_ENTITY("Legal Entity", ""),
    ORGANIZATIONAL_UNIT("Organizational Unit", ""),
    SITE("Site", ""),
    UNKNOWN("Unknown", "");

    override fun getTypeName(): String {
        return typeName
    }

    override fun getUrl(): String {
        return url
    }

    override fun getDefault(): BusinessPartnerType {
        return UNKNOWN
    }
}