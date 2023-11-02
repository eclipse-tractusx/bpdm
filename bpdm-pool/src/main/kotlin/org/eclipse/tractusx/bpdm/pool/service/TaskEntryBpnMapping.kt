/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.pool.entity.BpnRequestIdentifierMapping
import org.eclipse.tractusx.bpdm.pool.repository.BpnRequestIdentifierRepository
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceDto
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType
import org.eclipse.tractusx.orchestrator.api.model.TaskStepReservationEntryDto

class TaskEntryBpnMapping(taskEntries: List<TaskStepReservationEntryDto>, bpnRequestIdentifierRepository: BpnRequestIdentifierRepository) {

    private val bpnByRequestIdentifier:  MutableMap<String, String>
    private val createdBpnByRequestIdentifier:  MutableMap<String, String> = mutableMapOf()
    init{
        this.bpnByRequestIdentifier = readRequestMappings(taskEntries, bpnRequestIdentifierRepository)

    }

    private fun readRequestMappings(taskEntries: List<TaskStepReservationEntryDto>,  bpnRequestIdentifierRepository: BpnRequestIdentifierRepository ): MutableMap<String, String> {

        val references = taskEntries.mapNotNull { it.businessPartner.legalEntity?.bpnLReference } +
                taskEntries.mapNotNull { it.businessPartner.site?.bpnSReference } +
                taskEntries.mapNotNull { it.businessPartner.address?.bpnAReference }

        val usedRequestIdentifiers: Collection<String> = references.map { it.referenceValue }

        val mappings = bpnRequestIdentifierRepository.findDistinctByRequestIdentifierIn(usedRequestIdentifiers)
        val bpnByRequestIdentifier = mappings
            .map { it.requestIdentifier to it.bpn }
            .toMap()
        return bpnByRequestIdentifier.toMutableMap()
    }

    fun getBpn(bpnLReference: BpnReferenceDto): String? {

        return if (bpnLReference.referenceType == BpnReferenceType.BpnRequestIdentifier) {
            bpnByRequestIdentifier[bpnLReference.referenceValue]
        } else {
            bpnLReference.referenceValue
        }
    }

    fun addMapping(bpnLReference: BpnReferenceDto, bpn: String) {

        createdBpnByRequestIdentifier[bpnLReference.referenceValue] = bpn
        bpnByRequestIdentifier[bpnLReference.referenceValue] = bpn
    }

    fun writeCreatedMappingsToDb(bpnRequestIdentifierRepository: BpnRequestIdentifierRepository) {

        val mappingsToCreate = createdBpnByRequestIdentifier.map{
            BpnRequestIdentifierMapping(requestIdentifier=it.key, bpn =it.value)
        }
        bpnRequestIdentifierRepository.saveAll(mappingsToCreate)
    }

}