/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.bpdm.gate.mapper.orchestrator.outbound

import org.eclipse.tractusx.bpdm.gate.model.parsed.task.RelationTaskCandidateParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationType
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerRelations
import org.eclipse.tractusx.orchestrator.api.model.RelationValidityPeriod
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRelationsRequest
import org.eclipse.tractusx.orchestrator.api.model.TaskCreateRelationsRequestEntry
import org.eclipse.tractusx.orchestrator.api.model.TaskMode
import org.springframework.stereotype.Component
import org.eclipse.tractusx.orchestrator.api.model.RelationType as OrchestratorRelationType

/**
 * Maps relation golden record task requests onto the requests the Orchestrator receives.
 */
@Component
class RelationTaskRequestMapper {

    /**
     * Maps [candidates] onto one request creating a relation golden record task from the sharing member side for each
     * of them.
     */
    fun toCreateRequest(candidates: List<RelationTaskCandidateParsed>): TaskCreateRelationsRequest =
        TaskCreateRelationsRequest(
            mode = TaskMode.UpdateFromSharingMember,
            requests = candidates.map { TaskCreateRelationsRequestEntry(recordId = it.recordId, businessPartnerRelations = toRelations(it)) }
        )

    private fun toRelations(candidate: RelationTaskCandidateParsed): BusinessPartnerRelations =
        BusinessPartnerRelations(
            relationType = toOrchestratorType(candidate.relationType),
            businessPartnerSourceBpn = candidate.sourceBpn,
            businessPartnerTargetBpn = candidate.targetBpn,
            validityPeriods = candidate.validityPeriods.map { RelationValidityPeriod(validFrom = it.validFrom, validTo = it.validTo) },
            reasonCode = candidate.reasonCode
        )

    private fun toOrchestratorType(relationType: RelationType): OrchestratorRelationType =
        when (relationType) {
            RelationType.IsManagedBy -> OrchestratorRelationType.IsManagedBy
            RelationType.IsAlternativeHeadquarterFor -> OrchestratorRelationType.IsAlternativeHeadquarterFor
            RelationType.IsOwnedBy -> OrchestratorRelationType.IsOwnedBy
            RelationType.IsReplacedBy -> OrchestratorRelationType.IsReplacedBy
        }
}
