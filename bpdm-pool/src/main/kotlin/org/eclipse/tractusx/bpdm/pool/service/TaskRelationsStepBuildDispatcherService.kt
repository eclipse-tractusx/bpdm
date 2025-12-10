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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.orchestrator.api.model.RelationType
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepReservationEntryDto
import org.eclipse.tractusx.orchestrator.api.model.TaskRelationsStepResultEntryDto
import org.springframework.stereotype.Service

@Service
class TaskRelationsStepBuildDispatcherService(
    private val taskLegalEntityRelationsStepBuildService: TaskLegalEntityRelationsStepBuildService,
    private val taskAddressRelationsStepBuildService: TaskAddressRelationsStepBuildService
) {

    fun upsertBusinessPartnerRelations(taskEntry: TaskRelationsStepReservationEntryDto) : TaskRelationsStepResultEntryDto {
        val source = taskEntry.businessPartnerRelations.businessPartnerSourceBpn
        val target = taskEntry.businessPartnerRelations.businessPartnerTargetBpn
        val relationType = taskEntry.businessPartnerRelations.relationType

        return when{
            source.startsWith("BPNL", true) && target.startsWith("BPNL", true) && relationType in LEGAL_ENTITY_RELATION_TYPES-> {
                taskLegalEntityRelationsStepBuildService.upsertBusinessPartnerRelations(taskEntry)
            }
            source.startsWith("BPNA", true) && target.startsWith("BPNA", true) && relationType in ADDRESS_RELATION_TYPES-> {
                taskAddressRelationsStepBuildService.upsertAddressRelations(taskEntry)
            }
            else -> {
                throw BpdmValidationException("Invalid relation: mixed legal entity or address types not allowed (source=$source, target=$target)")
            }
        }
    }

    private val LEGAL_ENTITY_RELATION_TYPES = setOf(
        RelationType.IsAlternativeHeadquarterFor,
        RelationType.IsManagedBy,
        RelationType.IsOwnedBy
    )

    private val ADDRESS_RELATION_TYPES = setOf(
        RelationType.IsReplacedBy
    )
}