/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.orchestrator.entity

import jakarta.persistence.*
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.eclipse.tractusx.orchestrator.api.model.TaskStep
import org.hibernate.annotations.Type
import java.util.*

@Entity
@Table(
    name = "relations_golden_record_tasks",
    indexes = [
        Index(name = "index_relations_tasks_uuid", columnList = "uuid"),
        Index(name = "index_relations_tasks_step_step_state", columnList = "task_step,task_step_state"),
        Index(name = "index_relations_tasks_pending_timeout", columnList = "task_pending_timeout"),
        Index(name = "index_relations_tasks_retention_timeout", columnList = "task_retention_timeout"),
        Index(name = "index_relations_tasks_result_state_and_updated_at", columnList = "task_result_state, updated_at")
    ]
)
class RelationsGoldenRecordTaskDb(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bpdm_sequence")
    @SequenceGenerator(name = "bpdm_sequence", sequenceName = "bpdm_sequence", allocationSize = 1)
    @Column(name = "id", nullable = false, updatable = false, insertable = false)
    val id: Long = 0,

    @Column(name = "uuid", nullable = false, updatable = false, unique = true, columnDefinition = "uuid")
    val uuid: UUID = UUID.randomUUID(),

    @Column(updatable = false, nullable = false, name = "created_at")
    @Type(value = DbTimestampConverter::class)
    var createdAt: DbTimestamp,

    @Column(nullable = false, name = "updated_at")
    @Type(value = DbTimestampConverter::class)
    var updatedAt: DbTimestamp,
    @ManyToOne
    @JoinColumn(name = "gate_record_id", nullable = false, foreignKey = ForeignKey(name = "fk_tasks_gate_records"))
    var gateRecord: SharingMemberRecordDb,

    @Embedded
    val processingState: ProcessingState,
    @Embedded
    val businessPartnerRelations: BusinessPartnerRelations
) {

    fun updateBusinessPartnerRelations(newBusinessPartnerRelations: BusinessPartnerRelations) {
        with(newBusinessPartnerRelations) {
            BusinessPartnerRelations (
                relationType = relationType.also { businessPartnerRelations.relationType = it },
                businessPartnerSourceBpn = businessPartnerSourceBpn.also { businessPartnerRelations.businessPartnerSourceBpn = it },
                businessPartnerTargetBpn = businessPartnerTargetBpn.also { businessPartnerRelations.businessPartnerTargetBpn = it },
                reasonCode = reasonCode
            )
        }
    }

    @Embeddable
    class ProcessingState(
        @Enumerated(EnumType.STRING)
        @Column(name = "task_mode", nullable = false)
        var mode: TaskMode,
        @Enumerated(EnumType.STRING)
        @Column(name = "task_result_state", nullable = false)
        var resultState: ResultState,
        @ElementCollection(fetch = FetchType.LAZY)
        @CollectionTable(
            name = "relations_task_errors",
            joinColumns = [JoinColumn(name = "task_id", foreignKey = ForeignKey(name = "fk_errors_tasks"))],
            indexes = [Index(name = "index_task_errors_task_id", columnList = "task_id", unique = true)]
        )
        val errors: MutableList<RelationsTaskErrorDb>,
        @Enumerated(EnumType.STRING)
        @Column(name = "task_step", nullable = false)
        var step: TaskStep,
        @Enumerated(EnumType.STRING)
        @Column(name = "task_step_state", nullable = false)
        var stepState: StepState,
        @Type(value = DbTimestampConverter::class)
        @Column(name = "task_pending_timeout")
        var pendingTimeout: DbTimestamp?,
        @Type(value = DbTimestampConverter::class)
        @Column(name = "task_retention_timeout")
        var retentionTimeout: DbTimestamp?
    )

    @Embeddable
    class BusinessPartnerRelations(
        @Column(name = "relation_type", nullable = false)
        var relationType: RelationType,
        @Column(name = "source_bpn", nullable = false)
        var businessPartnerSourceBpn: String,
        @Column(name = "target_bpn", nullable = false)
        var businessPartnerTargetBpn: String,
        @ElementCollection(fetch = FetchType.LAZY)
        @CollectionTable(
            name = "relation_task_validity_periods",
            joinColumns = [JoinColumn(name = "relation_id", foreignKey = ForeignKey(name = "fk_task_relation_validity_periods_relation"))],
            indexes = [Index(name = "idx_relation_task_validity_periods_relation_id", columnList = "relation_id")]
        )
        var validityPeriods: MutableList<RelationValidityPeriod> = mutableListOf(),
        @Column(name = "reason_code", nullable = false)
        var reasonCode: String
    )


    enum class ResultState{
        Pending,
        Success,
        Error,
        Aborted
    }

    enum class StepState{
        Queued,
        Reserved,
        Success,
        Error,
        Aborted
    }

    enum class RelationType {
        IsAlternativeHeadquarterFor,
        IsManagedBy,
        IsOwnedBy,
        IsReplacedBy
    }

}