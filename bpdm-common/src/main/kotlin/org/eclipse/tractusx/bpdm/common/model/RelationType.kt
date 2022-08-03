package org.eclipse.tractusx.bpdm.common.model

enum class RelationType(private val typeName: String) : NamedType, HasDefaultValue<RelationType> {
    CX_LEGAL_SUCCESSOR_OF("Start is legal successor of End"),
    CX_LEGAL_PREDECESSOR_OF("Start is legal predecessor of End"),
    CX_ADDRESS_OF("Start is legally registered at End"),
    CX_SITE_OF("Start operates at site of End"),
    CX_OWNED_BY("Start is legally owned by End"),
    DIRECT_LEGAL_RELATION("Start is legally owned by End"),
    COMMERCIAL_ULTIMATE("End is highest commercial organization in hierarchy of Start"),
    DOMESTIC_BRANCH_RELATION("Start is domestic branch of End"),
    INTERNATIONAL_BRANCH_RELATION("Start is international branch of End"),
    DOMESTIC_LEGAL_ULTIMATE_RELATION("End is highest domestic organization in hierarchy of Start"),
    GLOBAL_LEGAL_ULTIMATE_RELATION("End is globally highest organization in hierarchy of Start"),
    LEGAL_PREDECESSOR("Start is legal predecessor of End"),
    LEGAL_SUCCESSOR("Start is legal successor of End"),
    DNB_PARENT("Start legally owns End"),
    DNB_HEADQUARTER("Start is legal headquarter of End"),
    DNB_DOMESTIC_ULTIMATE("End is highest domestic organization in hierarchy of Start"),
    DNB_GLOBAL_ULTIMATE("End is globally highest organization in hierarchy of Start"),
    LEI_DIRECT_PARENT("Start legally owns End"),
    LEI_INTERNATIONAL_BRANCH("Start is international branch of End"),
    LEI_ULTIMATE_PARENT("End is globally highest organization in hierarchy of Start"),
    UNKNOWN("Unknown");

    override fun getTypeName(): String {
        return typeName
    }

    override fun getDefault(): RelationType {
        return UNKNOWN
    }
}