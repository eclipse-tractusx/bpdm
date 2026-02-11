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

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.AddressRelationType
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.entity.AddressRelationDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.repository.AddressRelationRepository
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class HeadquarterSyncService(
    private val relationRepository: AddressRelationRepository,
    private val legalEntityRepository: LegalEntityRepository,
    private val changelogService: PartnerChangelogService
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun synchronizeHeadquarter(legalEntityDb: LegalEntityDb){
        val addressesToProcess = ArrayDeque<LogisticAddressDb>()
        val relocatedAddresses = mutableListOf<LogisticAddressDb>()
        val visitedValidityPeriods = mutableListOf<RelationValidityPeriod>()

        val today = LocalDate.now()

        addressesToProcess.add(legalEntityDb.legalAddress)

        do{
            val nextAddress = addressesToProcess.removeFirst()
            val replacingRelations =  relationRepository.findByTypeAndStartAddress(AddressRelationType.IsReplacedBy, nextAddress)

            val replacingRelationValidities =  replacingRelations
                .flatMap { relation -> relation.validityPeriods.map { RelationValidityPeriod(relation, it) } }

            val notVisitiedValidities = replacingRelationValidities
                .filterNot{ visitedValidityPeriods.contains(it) }

            val activeValidities = notVisitiedValidities
                .filter {  it.validityPeriod.validFrom <= today && (it.validityPeriod.validTo?: LocalDate.MAX) > today }

            activeValidities.forEach { visitedValidityPeriods.add(it) }

            val activeReplacingRelation = activeValidities.map { it.relation }.firstOrNull()

            if(activeReplacingRelation != null){
                relocatedAddresses.add(activeReplacingRelation.startAddress)
                relocatedAddresses.add(activeReplacingRelation.endAddress)
                addressesToProcess.add(activeReplacingRelation.endAddress)
            }
        }while (addressesToProcess.isNotEmpty())

        val newLegalAddress = relocatedAddresses.lastOrNull()

        if(newLegalAddress != null){
            val currentLegalAddress = legalEntityDb.legalAddress
            legalEntityDb.legalAddress = newLegalAddress

            legalEntityRepository.save(legalEntityDb)
            logger.info { "Updated legal address of legal entity '${legalEntityDb.bpn}': From '${currentLegalAddress.bpn}' to '${newLegalAddress.bpn}'" }

            changelogService.createChangelogEntry(ChangelogEntryCreateRequest(legalEntityDb.bpn, ChangelogType.UPDATE, BusinessPartnerType.LEGAL_ENTITY))
            changelogService.createChangelogEntry(ChangelogEntryCreateRequest(currentLegalAddress.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS))
            changelogService.createChangelogEntry(ChangelogEntryCreateRequest(newLegalAddress.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS))
        }
    }

    private data class RelationValidityPeriod(
        val relation: AddressRelationDb,
        val validityPeriod: RelationValidityPeriodDb
    )
}