package org.eclipse.tractusx.bpdm.common.model

enum class AddressType(private val typeName: String, private val url: String) : NamedUrlType, HasDefaultValue<AddressType> {
    BRANCH_OFFICE("Branch Office", ""),
    CARE_OF("Care of (c/o) Address", ""),
    HEADQUARTER("Headquarter", ""),
    LEGAL("Legal", ""),
    LEGAL_ALTERNATIVE("Legal Alternative", ""),
    PO_BOX("Post Office Box", ""),
    REGISTERED("Registered", ""),
    REGISTERED_AGENT_MAIL("Registered Agent Mail", ""),
    REGISTERED_AGENT_PHYSICAL("Registered Agent Physical", ""),
    VAT_REGISTERED("Vat Registered", ""),
    UNSPECIFIC("Unspecified", "");

    override fun getTypeName(): String {
        return typeName
    }

    override fun getUrl(): String {
        return url
    }

    override fun getDefault(): AddressType {
        return UNSPECIFIC
    }
}