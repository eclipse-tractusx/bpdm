package org.eclipse.tractusx.bpdm.common.model

enum class AdministrativeAreaType(private val areaName: String, private val url: String) : NamedUrlType, HasDefaultValue<AdministrativeAreaType> {
    COUNTY("County", ""),
    REGION("Region", ""),
    OTHER("Other", "");

    override fun getTypeName(): String {
        return areaName
    }

    override fun getUrl(): String {
        return url
    }

    override fun getDefault(): AdministrativeAreaType {
        return OTHER
    }
}