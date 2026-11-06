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

package org.eclipse.tractusx.bpdm.pool.service.operation.legalentity

import org.eclipse.tractusx.bpdm.pool.model.raw.relation.AddressRelationType
import org.eclipse.tractusx.bpdm.pool.entity.AddressRelationDb
import org.eclipse.tractusx.bpdm.pool.model.parsed.address.AddressContentChangeParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.legalentity.LegalEntityChangeParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.legalentity.LegalEntityHeaderChangeParsed
import org.eclipse.tractusx.bpdm.pool.model.parsed.legalentity.LegalEntityScriptVariantParsed
import org.eclipse.tractusx.bpdm.pool.model.raw.businesspartner.FieldUpdateRaw
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.RelationValidityPeriodDb
import org.eclipse.tractusx.bpdm.pool.repository.AddressRelationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Keeps a legal entity's legal address on the end of the address succession that is valid today.
 */
@Service
class HeadquarterSyncService(
    private val relationRepository: AddressRelationRepository,
    private val legalEntityUpdateService: LegalEntityUpdateService
) {

    /**
     * Relocates the legal address of [legalEntityDb] to the last address of today's succession chain starting at it, if
     * any, and drops the legal entity's script variants the new legal address does not cover.
     */
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
            val header = LegalEntityHeaderChangeParsed.NoOp.copy(
                relocatedLegalAddress = FieldUpdateRaw.Set(newLegalAddress),
                scriptVariants = coveredScriptVariants(legalEntityDb, newLegalAddress)
            )
            legalEntityUpdateService.update(listOf(LegalEntityChangeParsed(legalEntityDb, header, AddressContentChangeParsed.NoOp)))
        }
    }

    // Requests naming a legal entity in a script its legal address does not cover are rejected by the parsers; a
    // relocation has no request to reject, so the uncovered script variants are dropped instead.
    private fun coveredScriptVariants(legalEntity: LegalEntityDb, newLegalAddress: LogisticAddressDb): FieldUpdateRaw<List<LegalEntityScriptVariantParsed>> {
        val coveredScriptCodes = newLegalAddress.scriptCodes().toSet()
        val covered = legalEntity.scriptVariants.filter { it.scriptCode.technicalKey in coveredScriptCodes }
        if (covered.size == legalEntity.scriptVariants.size) return FieldUpdateRaw.NoOp

        return FieldUpdateRaw.Set(covered.map { LegalEntityScriptVariantParsed(it.scriptCode, it.legalName, it.shortName) })
    }

    private data class RelationValidityPeriod(
        val relation: AddressRelationDb,
        val validityPeriod: RelationValidityPeriodDb
    )
}