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

import org.eclipse.tractusx.bpdm.common.dto.AddressType
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.AddressRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.AddressRelationDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.ReasonCodeDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.exception.BpdmValidationException
import org.eclipse.tractusx.bpdm.pool.repository.AddressRelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AddressRelationUpsertService(
    private val addressRelationRepository: AddressRelationRepository,
    private val changelogService: PartnerChangelogService
): IAddressRelationUpsertStratergyService {

    @Transactional
    override fun upsertRelation(upsertRequest: IAddressRelationUpsertStratergyService.UpsertRequest): UpsertResult<AddressRelationDb> {
        val sourceAddress = upsertRequest.source
        val targetAddress = upsertRequest.target
        val addressRelationType = AddressRelationType.IsReplacedBy //As of now, only IsReplacedBy is supported for address relations
        val validityPeriods = upsertRequest.validityPeriods

        // Prevent self-referencing relations
        if (sourceAddress == targetAddress) {
            throw BpdmValidationException("A Address cannot have a relation to itself (BPNA: ${sourceAddress.bpn}).")
        }

        validateSoureAndTargetAddress(sourceAddress, targetAddress)

        val existingRelation = addressRelationRepository.findAll(
            AddressRelationRepository.byRelation(
                startAddress = sourceAddress,
                endAddress = targetAddress,
                type = addressRelationType
            )
        ).singleOrNull()

        val upsertResult = if (existingRelation != null) {
            // Update validity periods if changed
            if (validityPeriodsDiffer(existingRelation.validityPeriods, upsertRequest.validityPeriods)) {
                existingRelation.validityPeriods.clear()
                existingRelation.validityPeriods.addAll(upsertRequest.validityPeriods)
                addressRelationRepository.save(existingRelation)
                UpsertResult(existingRelation, UpsertType.Updated)
            } else {
                UpsertResult(existingRelation, UpsertType.NoChange)
            }
        } else {
            UpsertResult(createNewAddressRelation(
                UpsertRequest(
                    source = sourceAddress,
                    target = targetAddress,
                    addressRelationType = addressRelationType,
                    validityPeriods = validityPeriods,
                    reasonCode = upsertRequest.reasonCode
                )
            ), UpsertType.Created)
        }

        return upsertResult

    }

    private fun validateSoureAndTargetAddress(source: LogisticAddressDb, target: LogisticAddressDb) {
        if (source.legalEntity!!.bpn != target.legalEntity!!.bpn) {
            throw BpdmValidationException("Invalid 'IsReplacedBy' relation: The source address with BPNA '${source.bpn}' and target address with BPNA '${target.bpn}' do not belong to the same Legal Entity (BPNL '${source.legalEntity!!.bpn}' and '${target.legalEntity!!.bpn}'). "
                    + "Both addresses must belong to the same Legal Entity to create an 'IsReplacedBy' relation.")
        }
        if (getAddressType(source) != AddressType.LegalAddress) {
            throw BpdmValidationException("Invalid source address type for 'IsReplacedBy' relation: The source address with BPNA '${source.bpn}' is of type '${getAddressType(source)}'. "
                    + "Only addresses of type 'LegalAddress' can be the source of an 'IsReplacedBy' relation.")
        }
        if (getAddressType(target) != AddressType.AdditionalAddress) {
            throw BpdmValidationException("Invalid target address type for 'IsReplacedBy' relation: The target address with BPNA '${target.bpn}' is of type '${getAddressType(target)}'. "
                    + "Only addresses of type 'AdditionalAddress' can be the target of an 'IsReplacedBy' relation.")
        }
    }

    private fun createNewAddressRelation(upsertRequest: UpsertRequest): AddressRelationDb {
        val source = upsertRequest.source
        val target = upsertRequest.target
        val validityPeriods = upsertRequest.validityPeriods.map {
            RelationValidityPeriodDb(
                validFrom = it.validFrom,
                validTo = it.validTo
            )
        }.toMutableList()

        val newRelation = AddressRelationDb(
            type = upsertRequest.addressRelationType,
            startAddress = source,
            endAddress = target,
            validityPeriods = validityPeriods,
            reasonCode = upsertRequest.reasonCode
        )

        addressRelationRepository.save(newRelation)

        changelogService.createChangelogEntry(ChangelogEntryCreateRequest(source.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS))
        changelogService.createChangelogEntry(ChangelogEntryCreateRequest(target.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS))

        return newRelation
    }

    data class UpsertRequest(
        val source: LogisticAddressDb,
        val target: LogisticAddressDb,
        val addressRelationType: AddressRelationType,
        val validityPeriods: Collection<RelationValidityPeriodDb>,
        val reasonCode: ReasonCodeDb
    )

    private fun validityPeriodsDiffer(existingValidityPeriods: Collection<RelationValidityPeriodDb>, newValidityPeriods: Collection<RelationValidityPeriodDb>): Boolean {
        if (existingValidityPeriods.size != newValidityPeriods.size) return true
        return existingValidityPeriods.zip(newValidityPeriods).any { (e, n) ->
            e.validFrom != n.validFrom || e.validTo != n.validTo
        }
    }
}