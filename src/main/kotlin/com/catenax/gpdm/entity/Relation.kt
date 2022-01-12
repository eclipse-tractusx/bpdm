package com.catenax.gpdm.entity

import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "relations")
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

enum class RelationClass{
    CDQ_HIERARCHY,
    CDQ_TRANSITION,
    CX_HIERARCHY,
    DNB_HIERARCHY,
    LEI_HIERARCHY
}

enum class RelationType(val description: String){
    CX_LEGAL_SUCCESSOR_OF("Describes the legal relationship between a legal entity (successor) that takes over all or parts of the rights and obligations from one or more legal entities (predecessor)."),
    CX_LEGAL_PREDECESSOR_OF("Describes the legal relationship between a legal entity (predecessor) that transfers all or parts of its rights and obligations to one or more legal entities (successor)."),
    CX_ADDRESS_OF("Describes the relationship either between an address and one or more legal entities that are legally registered at this address, or between an address and the site that is located at this address."),
    CX_SITE_OF("Describes the relationship between a site and one or more legal entities that operate at this site."),
    CX_OWNED_BY("Describes the legal relationship between a legal entity (child) that is legally owned (by majority) by another legal entity (parent)."),
    DIRECT_LEGAL_RELATION("Describes the direct legal relationship in terms of a legal entity (parent) that owns the majority of another legal entity (child)."),
    COMMERCIAL_ULTIMATE("The highest commercial organization in the corporate family hierarchy."),
    DOMESTIC_BRANCH_RELATION("Domestic branches are units located in the same country as the headquarter."),
    INTERNATIONAL_BRANCH_RELATION("International branches are units located in different countries than the headquarter."),
    DOMESTIC_LEGAL_ULTIMATE_RELATION("Describes the relationship between the highest-level entity in the hierarchy tree, located in the same country as the entity with the lower position (direct or ultimate child)."),
    GLOBAL_LEGAL_ULTIMATE_RELATION("Describes the relationship between top level entity in the organization hierarchy and entities below."),
    LEGAL_PREDECESSOR("Describes the legal predecessor (end node) of a legal entity (start node) in terms of a backwards perspective on mergers, acquisitions, carve outs, etc."),
    LEGAL_SUCCESSOR("Describes the legal successor (end node) of a legal entity (start node) in terms of a forward perspective on mergers, acquisitions, carve outs, etc."),
    DNB_PARENT("Describes the direct legal relationship in terms of a legal entity (parent) that owns the majority of another legal entity (child)."),
    DNB_HEADQUARTER("Describes the legal relationship between an entity (headquarter) and its branches."),
    DNB_DOMESTIC_ULTIMATE("Describes the relationship between the highest-level entity in the hierarchy tree, located in the same country as the entity with the lower position (direct or ultimate child)."),
    DNB_GLOBAL_ULTIMATE("Describes the relationship between top level entity in the organization hierarchy and entities below."),
    LEI_DIRECT_PARENT("Describes the direct legal relationship in terms of a legal entity (parent) that owns the majority of another legal entity (child)."),
    LEI_INTERNATIONAL_BRANCH("International branches are units that could be located in the same or in different countries than headquarter."),
    LEI_ULTIMATE_PARENT("Describes the relationship between top-level company in the organization hierarchy and entities below."),
}