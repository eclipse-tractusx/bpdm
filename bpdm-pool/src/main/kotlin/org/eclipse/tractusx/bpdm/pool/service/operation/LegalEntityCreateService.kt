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

package org.eclipse.tractusx.bpdm.pool.service.operation

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.LegalEntityEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.AddressCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityContentParsed
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityCreateParseError
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityCreateRequest
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.combine
import org.eclipse.tractusx.bpdm.pool.model.parseAndExecute
import org.eclipse.tractusx.bpdm.pool.model.zipParseResults
import org.eclipse.tractusx.bpdm.pool.repository.LegalEntityRepository
import org.eclipse.tractusx.bpdm.pool.service.BpnIssuingService
import org.eclipse.tractusx.bpdm.pool.service.parser.AddressContentParser
import org.eclipse.tractusx.bpdm.pool.service.parser.LegalEntityHeaderParser
import org.eclipse.tractusx.bpdm.pool.service.parser.LegalEntityIdentifierDuplicateValidator
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

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
/**
 * Creates legal entities, the top of the business-partner hierarchy. Unlike sites and additional addresses there is no
 * parent to resolve — `parse` only validates header + identifier-uniqueness + legal-address content (each by a single-
 * responsibility validator, combined with `combine`/`zipParseResults`); `create` issues the BPN and persists the legal
 * entity and its legal address. The legal address (whose parent is the still-unsaved legal entity) is delegated to the
 * parent-injected [AddressCreateService]. Order-preserving positional contract (see [org.eclipse.tractusx.bpdm.pool.model.ParseResult]).
 */
@Service
class LegalEntityCreateService(
    private val legalEntityHeaderParser: LegalEntityHeaderParser,
    private val duplicateValidator: LegalEntityIdentifierDuplicateValidator,
    private val addressContentParser: AddressContentParser,
    private val addressCreateService: AddressCreateService,
    private val bpnIssuingService: BpnIssuingService,
    private val legalEntityRepository: LegalEntityRepository,
    private val changelogService: PartnerChangelogService,
    private val legalEntityEntityMapper: LegalEntityEntityMapper
) {

    fun parse(requests: List<LegalEntityCreateRequest>): List<ParseResult<LegalEntityCreateParsed, LegalEntityCreateParseError>> {
        val headers = requests.map { it.content.header }
        val headerResults = legalEntityHeaderParser.parse(headers)
        // No owner BPN exists yet on create, so every identifier collides only within the request batch or against the DB.
        val duplicateErrors = duplicateValidator.validate(headers, headers.map { null })
        val mergedHeaderResults = headerResults.zip(duplicateErrors) { result, extra -> result.combine(extra) { it } }

        val legalAddresses = requests.map { it.content.legalAddress }
        val legalAddressResults = addressContentParser.parse(legalAddresses, legalAddresses.map { null })

        return zipParseResults(mergedHeaderResults, legalAddressResults) { header, legalAddress ->
            LegalEntityCreateParsed(LegalEntityContentParsed(header, legalAddress))
        }
    }

    /**
     * Returns the persisted entities (within the caller's transaction) rather than a detached response model: building
     * version-specific responses is the job of the border/application service at the edge.
     */
    @Transactional
    fun create(parsed: List<LegalEntityCreateParsed>): List<LegalEntityDb> {
        val bpns = bpnIssuingService.issueLegalEntityBpns(parsed.size)
        val currentness = Instant.now().truncatedTo(ChronoUnit.MICROS)
        // A new legal entity's confidence starts with zero sharing members (preserves the previous create behavior).
        val legalEntities = parsed.zip(bpns) { entry, bpn ->
            legalEntityEntityMapper.toEntity(bpn, entry.content.header, currentness, numberOfSharingMembers = 0)
        }

        // Emit the legal entity changelog before the address create service emits the ADDRESS CREATE changelog, so the
        // overall changelog order stays "legal entity, then its legal address".
        changelogService.createChangelogEntries(legalEntities.map {
            ChangelogEntryCreateRequest(it.bpn, ChangelogType.CREATE, BusinessPartnerType.LEGAL_ENTITY)
        })

        // The legal address's parent is the still-unsaved legal entity (no site); it flushes in the right order at commit
        // thanks to the nullable back-FK and order_inserts. The address create service owns the address BPN + changelog.
        val legalAddresses = addressCreateService.create(parsed.zip(legalEntities).map { (entry, legalEntity) ->
            val legalAddress = entry.content.legalAddress
            AddressCreateParsed(legalEntity, site = null, legalAddress.address, legalAddress.scriptVariants)
        })
        legalEntities.zip(legalAddresses).forEach { (legalEntity, address) -> legalEntity.legalAddress = address }

        legalEntityRepository.saveAll(legalEntities)
        return legalEntities
    }

    @Transactional
    fun parseAndCreate(requests: List<LegalEntityCreateRequest>): List<ParseResult<LegalEntityDb, LegalEntityCreateParseError>> =
        parseAndExecute(requests, ::parse, ::create)
}