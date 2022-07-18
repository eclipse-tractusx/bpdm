package org.eclipse.tractusx.bpdm.common.model

enum class LocalityType(private val typeName: String, private val url: String) : NamedUrlType, HasDefaultValue<LocalityType> {
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