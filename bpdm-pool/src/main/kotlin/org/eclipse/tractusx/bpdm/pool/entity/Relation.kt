package org.eclipse.tractusx.bpdm.pool.entity

import org.eclipse.tractusx.bpdm.common.model.RelationClass
import org.eclipse.tractusx.bpdm.common.model.RelationType
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

