package org.eclipse.tractusx.bpdm.common.model

enum class BusinessStatusType(private val statusName: String, private val url: String) : NamedUrlType, HasDefaultValue<BusinessStatusType> {
    ACTIVE("Active", ""),
    DISSOLVED("Dissolved", ""),
    IN_LIQUIDATION("In Liquidation", ""),
    INACTIVE("Inactive", ""),
    INSOLVENCY("Insolvency", ""),
    UNKNOWN("Unknown", "");

    override fun getTypeName(): String {
        return statusName
    }

    override fun getUrl(): String {
        return url
    }

    override fun getDefault(): BusinessStatusType {
        return UNKNOWN
    }
}