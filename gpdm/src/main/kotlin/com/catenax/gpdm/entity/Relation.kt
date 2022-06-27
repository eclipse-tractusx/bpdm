package com.catenax.gpdm.entity

import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "relations",
    indexes = [
        Index(columnList = "start_node_id"),
        Index(columnList = "end_node_id")
    ])
class Relation (
    @Column(name = "class", nullable = false)
    @Enumerated(EnumType.STRING)
    val relationClass: RelationClass,
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val type: RelationType,
    @ManyToOne
    @JoinColumn(name = "start_node_id", nullable = false)
    val startNode: BusinessPartner,
    @ManyToOne
    @JoinColumn(name = "end_node_id", nullable = false)
    val endNode: BusinessPartner,
    @Column(name = "started_at")
    val startedAt: LocalDateTime?,
    @Column(name = "ended_at")
    val endedAt: LocalDateTime?
        ) : BaseEntity()

enum class RelationClass(private val typeName: String): NamedType{
    CDQ_HIERARCHY("CDQ Hierarchy"),
    CDQ_TRANSITION("CDQ Transition"),
    CX_HIERARCHY("Catena-X"),
    DNB_HIERARCHY("DNB"),
    LEI_HIERARCHY("LEI");

    override fun getTypeName(): String {
        return typeName
    }
}

enum class RelationType(private val typeName: String): NamedType{
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
    DNB_PARENT( "Start legally owns End"),
    DNB_HEADQUARTER("Start is legal headquarter of End"),
    DNB_DOMESTIC_ULTIMATE("End is highest domestic organization in hierarchy of Start"),
    DNB_GLOBAL_ULTIMATE("End is globally highest organization in hierarchy of Start"),
    LEI_DIRECT_PARENT("Start legally owns End"),
    LEI_INTERNATIONAL_BRANCH("Start is international branch of End"),
    LEI_ULTIMATE_PARENT("End is globally highest organization in hierarchy of Start");

    override fun getTypeName(): String {
        return typeName
    }
}