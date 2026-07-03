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
import org.eclipse.tractusx.bpdm.common.util.replace
import org.eclipse.tractusx.bpdm.pool.api.model.ChangelogType
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.dto.UpsertResult
import org.eclipse.tractusx.bpdm.pool.dto.UpsertType
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.mapper.entity.AddressEntityMapper
import org.eclipse.tractusx.bpdm.pool.model.AddressContentParseError
import org.eclipse.tractusx.bpdm.pool.model.AddressContentParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressContentRequest
import org.eclipse.tractusx.bpdm.pool.model.AddressContentUpdateParsed
import org.eclipse.tractusx.bpdm.pool.model.AddressScriptVariantParsed
import org.eclipse.tractusx.bpdm.pool.model.LogisticAddressParsed
import org.eclipse.tractusx.bpdm.pool.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.combine
import org.eclipse.tractusx.bpdm.pool.repository.LogisticAddressRepository
import org.eclipse.tractusx.bpdm.pool.service.parser.AddressIdentifierDuplicateValidator
import org.eclipse.tractusx.bpdm.pool.service.BusinessPartnerEquivalenceMapper
import org.eclipse.tractusx.bpdm.pool.service.LogisticAddressRequestParser
import org.eclipse.tractusx.bpdm.pool.service.PartnerChangelogService
import org.springframework.stereotype.Service

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
@Service
class AddressContentUpdateService(
    private val logisticAddressRepository: LogisticAddressRepository,
    private val changelogService: PartnerChangelogService,
    private val equivalenceMapper: BusinessPartnerEquivalenceMapper,
    private val addressEntityMapper: AddressEntityMapper,
    private val addressRequestParser: LogisticAddressRequestParser,
    private val duplicateValidator: AddressIdentifierDuplicateValidator
)
{
    /**
     * Validates address content only (presence/format, metadata resolution, identifier duplicates). [ownerBpns] is the
     * BPN of each entry's update target, so an address may legitimately re-submit its own existing identifiers.
     */
    fun parse(contents: List<AddressContentRequest>, ownerBpns: List<String?>): List<ParseResult<AddressContentParsed, AddressContentParseError>> {
        val contentResults = addressRequestParser.parse(contents)
        val duplicateErrors = duplicateValidator.validate(contents, ownerBpns)
        return contentResults.mapIndexed { index, result -> result.combine(duplicateErrors[index]) { it } }
    }


    fun update(requests: List<AddressContentUpdateParsed>): List<UpsertResult<LogisticAddressDb>>{
        return requests.map { update(it) }
    }

    private fun update(request: AddressContentUpdateParsed): UpsertResult<LogisticAddressDb> {
        val target = request.target

        val before = equivalenceMapper.toEquivalenceDto(target)
        doUpdateContent(target, request.address, request.scriptVariants, target.confidenceCriteria.numberOfSharingMembers)
        val contentChanged = equivalenceMapper.toEquivalenceDto(target) != before

        if (contentChanged) {
            logisticAddressRepository.save(target)

            if (request.createChangelog)
                changelogService.createChangelogEntries(listOf(ChangelogEntryCreateRequest(target.bpn, ChangelogType.UPDATE, BusinessPartnerType.ADDRESS)))
        }

        return UpsertResult(target, if (contentChanged) UpsertType.Updated else UpsertType.NoChange)
    }


    private fun doUpdateContent(target: LogisticAddressDb, address: LogisticAddressParsed, scriptVariants: List<AddressScriptVariantParsed>, numberOfSharingMembers: Int){
        target.name = address.name
        target.physicalPostalAddress = addressEntityMapper.toPhysical(address.physicalPostalAddress)
        target.alternativePostalAddress = address.alternativePostalAddress?.let { addressEntityMapper.toAlternative(it) }
        target.confidenceCriteria = addressEntityMapper.toConfidence(address.confidenceCriteria, numberOfSharingMembers)
        target.identifiers.replace(addressEntityMapper.toIdentifiers(address.identifiers, target))
        target.states.replace(addressEntityMapper.toStates(address.states, target))
        target.scriptVariants.replace(addressEntityMapper.toScriptVariants(scriptVariants))
    }

}