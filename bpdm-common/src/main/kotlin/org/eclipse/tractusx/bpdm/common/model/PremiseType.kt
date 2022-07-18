package org.eclipse.tractusx.bpdm.common.model

enum class PremiseType(private val typeName: String, private val url: String) : NamedUrlType, HasDefaultValue<PremiseType> {
    BUILDING("Building", ""),
    OTHER("Other type", ""),
    LEVEL("Level", ""),
    HARBOUR("Harbour", ""),
    ROOM("Room", ""),
    SUITE("Suite", ""),
    UNIT("Unit", ""),
    WAREHOUSE("Warehouse", "");

    override fun getTypeName(): String {
        return typeName
    }

    override fun getUrl(): String {
        return url
    }

    override fun getDefault(): PremiseType {
        return OTHER
    }
}