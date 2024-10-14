/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.orchestrator.api.model.*
import org.hibernate.annotations.Type
import java.util.*

@Entity
@Table(
    name = "golden_record_tasks",
    indexes = [
        Index(name = "index_tasks_uuid", columnList = "uuid"),
        Index(name = "index_tasks_step_step_state", columnList = "task_step,task_step_state"),
        Index(name = "index_tasks_pending_timeout", columnList = "task_pending_timeout"),
        Index(name = "index_tasks_retention_timeout", columnList = "task_retention_timeout"),
        Index(name = "index_tasks_result_state_and_updated_at", columnList = "task_result_state, updated_at")
    ]
)
class GoldenRecordTaskDb(
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
    var gateRecord: GateRecordDb,

    @Embedded
    val processingState: ProcessingState,
    @Embedded
    val businessPartner: BusinessPartner
) {
    fun updateBusinessPartner(newBusinessPartnerData: BusinessPartner){
        with(newBusinessPartnerData){
            //Trick to make sure on a change to the business partner model
            // we get a compile error if we don't also adjust this update method
            // We discard the created business partner object afterward, it is just there for this check
            BusinessPartner(
                nameParts = nameParts.also { businessPartner.nameParts.replace(it) },
                identifiers = identifiers.also { businessPartner.identifiers.replace(it) },
                businessStates = businessStates.also { businessPartner.businessStates.replace(it) },
                confidences = confidences.also { businessPartner.confidences.replace(it) },
                addresses = addresses.also { businessPartner.addresses.replace(it) },
                bpnReferences = bpnReferences.also { businessPartner.bpnReferences.replace(it) },
                legalName = legalName.also { businessPartner.legalName = it },
                legalShortName = legalShortName.also { businessPartner.legalShortName = it },
                siteExists = siteExists.also { businessPartner.siteExists = it },
                siteName = siteName.also { businessPartner.siteName = it },
                legalForm = legalForm.also { businessPartner.legalForm = it },
                isCatenaXMemberData = isCatenaXMemberData.also { businessPartner.isCatenaXMemberData = it },
                owningCompany = owningCompany.also { businessPartner.owningCompany = it },
                legalEntityHasChanged = legalEntityHasChanged.also { businessPartner.legalEntityHasChanged = it },
                siteHasChanged = siteHasChanged.also { businessPartner.siteHasChanged = it }
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
            name = "task_errors",
            joinColumns = [JoinColumn(name = "task_id", foreignKey = ForeignKey(name = "fk_errors_tasks"))],
            indexes = [Index(name = "index_task_errors_task_id", columnList = "task_id", unique = true)]
        )
        val errors: MutableList<TaskErrorDb>,
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
    class BusinessPartner(
        @ElementCollection(fetch = FetchType.LAZY)
        @OrderColumn(name = "index", nullable = false)
        @CollectionTable(
            name = "business_partner_name_parts",
            joinColumns = [JoinColumn(name = "task_id", foreignKey = ForeignKey(name = "fk_name_parts_tasks"))],
            indexes = [Index(name = "index_name_parts_task_id", columnList = "task_id", unique = true)]
        )
        val nameParts: MutableList<NamePartDb>,
        @ElementCollection(fetch = FetchType.LAZY)
        @OrderColumn(name = "index", nullable = false)
        @CollectionTable(
            name = "business_partner_identifiers",
            joinColumns = [JoinColumn(name = "task_id", foreignKey = ForeignKey(name = "fk_identifiers_tasks"))],
            indexes = [Index(name = "index_identifiers_task_id", columnList = "task_id", unique = true)]
        )
        val identifiers: MutableList<IdentifierDb>,
        @ElementCollection(fetch = FetchType.LAZY)
        @OrderColumn(name = "index", nullable = false)
        @CollectionTable(
            name = "business_partner_states",
            joinColumns = [JoinColumn(name = "task_id", foreignKey = ForeignKey(name = "fk_states_tasks"))],
            indexes = [Index(name = "index_business_states_task_id", columnList = "task_id", unique = true)]
        )
        val businessStates: MutableList<BusinessStateDb>,
        @ElementCollection(fetch = FetchType.LAZY)
        @MapKeyColumn(name = "scope")
        @MapKeyEnumerated(EnumType.STRING)
        @CollectionTable(
            name = "business_partner_confidences",
            joinColumns = [JoinColumn(name = "task_id", foreignKey = ForeignKey(name = "fk_confidences_tasks"))],
            uniqueConstraints = [UniqueConstraint(name = "uc_business_partner_confidences_task_scope", columnNames = ["task_id", "scope"])],
            indexes = [Index(name = "index_confidences_task_id", columnList = "task_id", unique = true)]
        )
        val confidences: MutableMap<ConfidenceCriteriaDb.Scope, ConfidenceCriteriaDb>,
        @ElementCollection(fetch = FetchType.LAZY)
        @MapKeyColumn(name = "scope")
        @MapKeyEnumerated(EnumType.STRING)
        @CollectionTable(
            name = "business_partner_addresses",
            joinColumns = [JoinColumn(name = "task_id", foreignKey = ForeignKey(name = "fk_addresses_tasks"))],
            uniqueConstraints = [UniqueConstraint(name = "uc_business_partner_addresses_task_scope", columnNames = ["task_id", "scope"])],
            indexes = [Index(name = "index_addresses_task_id", columnList = "task_id", unique = true)]
        )
        val addresses: MutableMap<PostalAddressDb.Scope, PostalAddressDb>,
        @ElementCollection(fetch = FetchType.LAZY)
        @MapKeyColumn(name = "scope")
        @MapKeyEnumerated(EnumType.STRING)
        @CollectionTable(
            name = "business_partner_bpn_references",
            joinColumns = [JoinColumn(name = "task_id", foreignKey = ForeignKey(name = "fk_bpn_references_tasks"))],
            uniqueConstraints = [UniqueConstraint(name = "uc_business_partner_bpn_references_task_scope", columnNames = ["task_id", "scope"])],
            indexes = [Index(name = "index_bpn_references_task_id", columnList = "task_id", unique = true)]
        )
        val bpnReferences: MutableMap<BpnReferenceDb.Scope, BpnReferenceDb>,
        @Column(name = "legal_name")
        var legalName: String?,
        @Column(name = "legal_short_name")
        var legalShortName: String?,
        @Column(name = "site_exists", nullable = false)
        var siteExists: Boolean,
        @Column(name = "site_name")
        var siteName: String?,
        @Column(name = "legal_form")
        var legalForm: String?,
        @Column(name = "is_cx_member")
        var isCatenaXMemberData: Boolean?,
        @Column(name = "owning_company_bpnl")
        var owningCompany: String?,
        @Column(name = "legal_entity_has_changed")
        var legalEntityHasChanged: Boolean?,
        @Column(name = "site_has_changed")
        var siteHasChanged: Boolean?
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
}


