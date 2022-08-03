package org.eclipse.tractusx.bpdm.common.model

enum class RelationClass(private val typeName: String) : NamedType, HasDefaultValue<RelationClass> {
    CDQ_HIERARCHY("CDQ Hierarchy"),
    CDQ_TRANSITION("CDQ Transition"),
    CX_HIERARCHY("Catena-X"),
    DNB_HIERARCHY("DNB"),
    LEI_HIERARCHY("LEI"),
    UNKNOWN("Unknown");

    override fun getTypeName(): String {
        return typeName
    }

    override fun getDefault(): RelationClass {
        return UNKNOWN
    }
}