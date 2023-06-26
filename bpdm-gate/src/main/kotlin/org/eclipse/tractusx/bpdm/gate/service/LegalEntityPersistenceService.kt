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


import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.model.OutputInputEnum
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.repository.GateAddressRepository
import org.eclipse.tractusx.bpdm.gate.repository.LegalEntityRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LegalEntityPersistenceService(
    private val gateLegalEntityRepository: LegalEntityRepository,
    private val gateAddressRepository: GateAddressRepository
) {

    @Transactional
    fun persistLegalEntitiesBP(legalEntities: Collection<LegalEntityGateInputRequest>, datatype: OutputInputEnum) {

        //finds Legal Entity by External ID
        val legalEntityRecord = gateLegalEntityRepository.findDistinctByExternalIdIn(legalEntities.map { it.externalId })

        //Business Partner persist
        legalEntities.forEach { legalEntity ->
            val fullLegalEntity = legalEntity.toLegalEntity(datatype)
            legalEntityRecord.find { it.externalId == legalEntity.externalId && it.dataType == datatype }?.let { existingLegalEntity ->

                val logisticAddressRecord =
                    gateAddressRepository.findByExternalIdAndDataType(getMainAddressForLegalEntityExternalId(existingLegalEntity.externalId), datatype)
                        ?: throw BpdmNotFoundException("Business Partner", "Error")

                updateAddress(logisticAddressRecord, fullLegalEntity.legalAddress)

                updateLegalEntity(existingLegalEntity, legalEntity, logisticAddressRecord)
                gateLegalEntityRepository.save(existingLegalEntity)

            } ?: run {

                gateLegalEntityRepository.save(fullLegalEntity)
            }

        }
    }

    private fun updateLegalEntity(
        legalEntity: LegalEntity,
        legalEntityRequest: LegalEntityGateInputRequest,
        logisticAddressRecord: LogisticAddress
    ): LegalEntity {
        legalEntity.externalId = legalEntityRequest.externalId
        legalEntity.legalForm = legalEntityRequest.legalEntity.legalForm
        legalEntity.legalName = Name(value = legalEntityRequest.legalNameParts[0], shortName = legalEntityRequest.legalEntity.legalShortName)
        legalEntity.identifiers.replace(legalEntityRequest.legalEntity.identifiers.map { toEntityIdentifier(it, legalEntity) })
        legalEntity.states.replace(legalEntityRequest.legalEntity.states.map { toEntityState(it, legalEntity) })
        legalEntity.classifications.replace(legalEntityRequest.legalEntity.classifications.map { toEntityClassification(it, legalEntity) })
        legalEntity.legalAddress = logisticAddressRecord
        legalEntity.legalAddress.legalEntity = legalEntity
        return legalEntity
    }

    private fun updateAddress(address: LogisticAddress, changeAddress: LogisticAddress) {

        address.name = changeAddress.name
        address.externalId = changeAddress.externalId
        address.legalEntity = changeAddress.legalEntity
        address.siteExternalId = changeAddress.siteExternalId
        address.physicalPostalAddress = changeAddress.physicalPostalAddress
        address.alternativePostalAddress = changeAddress.alternativePostalAddress

        address.identifiers.replace(changeAddress.identifiers.map { toEntityIdentifier(it, address) })
        address.states.replace(changeAddress.states.map { toEntityAddress(it, address) })

    }

    fun toEntityAddress(dto: AddressState, address: LogisticAddress): AddressState {
        return AddressState(dto.description, dto.validFrom, dto.validTo, dto.type, address)
    }

    fun toEntityIdentifier(dto: AddressIdentifier, address: LogisticAddress): AddressIdentifier {
        return AddressIdentifier(dto.value, dto.type, address)
    }

}