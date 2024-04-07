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

package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.pool.entity.BpnRequestIdentifierMappingDb
import org.eclipse.tractusx.bpdm.pool.repository.BpnRequestIdentifierRepository
import org.eclipse.tractusx.orchestrator.api.model.BpnReferenceType
import org.eclipse.tractusx.orchestrator.api.model.TaskStepReservationEntryDto
import org.eclipse.tractusx.orchestrator.api.model.BpnReference

class TaskEntryBpnMapping(taskEntries: List<TaskStepReservationEntryDto>, bpnRequestIdentifierRepository: BpnRequestIdentifierRepository) {

    private val bpnByRequestIdentifier:  MutableMap<String, String>
    private val createdBpnByRequestIdentifier:  MutableMap<String, String> = mutableMapOf()
    init{
        this.bpnByRequestIdentifier = readRequestMappings(taskEntries, bpnRequestIdentifierRepository)

    }

    private fun readRequestMappings(taskEntries: List<TaskStepReservationEntryDto>,  bpnRequestIdentifierRepository: BpnRequestIdentifierRepository ): MutableMap<String, String> {

        val references = taskEntries.map { it.businessPartner.legalEntity.bpnReference } +
                taskEntries.map { it.businessPartner.legalEntity.legalAddress.bpnReference } +
                taskEntries.mapNotNull { it.businessPartner.site?.bpnReference } +
                taskEntries.mapNotNull { it.businessPartner.site?.siteMainAddress?.bpnReference } +
                taskEntries.mapNotNull { it.businessPartner.additionalAddress?.bpnReference }
                    .filter { it.referenceValue == null || it.referenceType == null }

        val usedRequestIdentifiers: Collection<String> = references
            .filter { it.referenceType == BpnReferenceType.BpnRequestIdentifier }
            .map { it.referenceValue!! }

        val mappings = bpnRequestIdentifierRepository.findDistinctByRequestIdentifierIn(usedRequestIdentifiers)
        val bpnByRequestIdentifier = mappings.associate { it.requestIdentifier to it.bpn }
        return bpnByRequestIdentifier.toMutableMap()
    }

    fun getBpn(bpnReference: BpnReference?): String? {

        return if(bpnReference == null) {
            null
        } else if (bpnReference.referenceType == BpnReferenceType.BpnRequestIdentifier) {
            bpnByRequestIdentifier[bpnReference.referenceValue]
        } else {
            bpnReference.referenceValue
        }
    }

    fun hasBpnFor(bpnReference: BpnReference?): Boolean {

        return bpnReference != null && (bpnReference.referenceType == BpnReferenceType.Bpn
                ||  (bpnReference.referenceType == BpnReferenceType.BpnRequestIdentifier
                && bpnByRequestIdentifier.containsKey(bpnReference.referenceValue)))
    }


    fun addMapping(bpnReference: BpnReference, bpn: String) {
        bpnReference.takeIf { it.referenceType == BpnReferenceType.BpnRequestIdentifier}
            ?.referenceValue?.takeIf { it.isNotEmpty() }
            ?.let {
                createdBpnByRequestIdentifier[it] = bpn
                bpnByRequestIdentifier[it] = bpn
            }
    }

    fun writeCreatedMappingsToDb(bpnRequestIdentifierRepository: BpnRequestIdentifierRepository) {

        val mappingsToCreate = createdBpnByRequestIdentifier.map{
            BpnRequestIdentifierMappingDb(requestIdentifier = it.key, bpn = it.value)
        }
        bpnRequestIdentifierRepository.saveAll(mappingsToCreate)
    }

}