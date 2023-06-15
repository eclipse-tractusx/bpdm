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
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.LsaType
import org.eclipse.tractusx.bpdm.gate.entity.*
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.eclipse.tractusx.bpdm.gate.repository.GateAddressRepository
import org.eclipse.tractusx.bpdm.gate.repository.LegalEntityRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class LegalEntityPersistenceService(
    private val gateLegalEntityRepository: LegalEntityRepository,
    private val gateAddressRepository: GateAddressRepository,
    private val changelogRepository: ChangelogRepository
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
                saveChangelog(legalEntity)
            } ?: run {
                gateLegalEntityRepository.save(fullLegalEntity)
                saveChangelog(legalEntity)
            }
        }
    }

    //Creates Changelog For both Legal Entity and Logistic Address when they are created or updated
    private fun saveChangelog(legalEntity: LegalEntityGateInputRequest) {
        changelogRepository.save(ChangelogEntry(getMainAddressForLegalEntityExternalId(legalEntity.externalId), LsaType.ADDRESS))
        changelogRepository.save(ChangelogEntry(legalEntity.externalId, LsaType.LEGAL_ENTITY))
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

    @Transactional
    fun persistLegalEntitiesOutputBP(legalEntities: Collection<LegalEntityGateOutputRequest>, datatype: OutputInputEnum) {
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
                updateLegalEntityOutput(existingLegalEntity, legalEntity, logisticAddressRecord)

                gateLegalEntityRepository.save(existingLegalEntity)
            } ?: run {
                if (legalEntityRecord.find { it.externalId == fullLegalEntity.externalId && it.dataType == OutputInputEnum.Input } == null) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Input Legal Entity doesn't exist")
                } else {
                    gateLegalEntityRepository.save(fullLegalEntity)
                }
            }
        }
    }

    private fun updateLegalEntityOutput(
        legalEntity: LegalEntity,
        legalEntityRequest: LegalEntityGateOutputRequest,
        logisticAddressRecord: LogisticAddress
    ): LegalEntity {
        legalEntity.bpn = legalEntityRequest.bpn
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

}