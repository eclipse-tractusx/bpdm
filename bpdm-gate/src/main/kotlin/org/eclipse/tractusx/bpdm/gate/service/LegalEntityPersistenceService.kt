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

package org.eclipse.tractusx.bpdm.gate.service


import org.eclipse.tractusx.bpdm.gate.entity.LegalEntity
import org.eclipse.tractusx.bpdm.gate.repository.LegalEntityRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.common.util.replace
@Service
class LegalEntityPersistenceService(
    private val gateLegalEntityRepository: LegalEntityRepository
) {

    @Transactional
    fun persistLegalEntytiesBP(legalEntities: Collection<LegalEntityGateInputRequest>) {

        //finds Legal Entity by External ID
        val legalEntityRecord  = gateLegalEntityRepository.findDistinctByBpnIn(legalEntities.map { it.externalId })

        //Business Partner persist
        legalEntities.forEach { legalEntity ->
            val fullLegalEntity = legalEntity.toLegalEntity()
            legalEntityRecord.find { it.externalId == legalEntity.externalId }?.let { existingLegalEntity ->
                updateLegalEntity(existingLegalEntity, legalEntity)
                gateLegalEntityRepository.save(existingLegalEntity)
            } ?: run {
                gateLegalEntityRepository.save(fullLegalEntity)
            }

        }
    }


    private fun updateLegalEntity(legalEntity: LegalEntity, legalEntityRequest: LegalEntityGateInputRequest): LegalEntity {
        legalEntity.bpn = legalEntityRequest.bpn.toString();
        legalEntity.externalId = legalEntityRequest.externalId;
        legalEntity.legalForm = legalEntityRequest.legalEntity.legalForm.toString();
        legalEntity.identifiers.replace(legalEntityRequest.legalEntity.identifiers.map { toEntityIdentifier(it,legalEntity) }.toSet());
        legalEntity.legalName= legalEntityRequest.legalEntity.legalName.toName();
        legalEntity.identifiers.replace(legalEntityRequest.legalEntity.identifiers.map{ toEntityIdentifier(it,legalEntity) }.toSet());
        legalEntity.states.replace(legalEntityRequest.legalEntity.states.map{toEntityState(it,legalEntity) }.toSet());
        legalEntity.classifications.replace(legalEntityRequest.legalEntity.classifications.map { toEntityClassification(it, legalEntity) }.toSet())
        return legalEntity;
    }

}