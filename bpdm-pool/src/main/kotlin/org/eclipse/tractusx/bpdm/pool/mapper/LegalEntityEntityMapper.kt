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

package org.eclipse.tractusx.bpdm.pool.mapper

import org.eclipse.tractusx.bpdm.pool.entity.ConfidenceCriteriaDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityIdentifierDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityScriptVariantDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityStateDb
import org.eclipse.tractusx.bpdm.pool.entity.NameDb
import org.eclipse.tractusx.bpdm.pool.model.ConfidenceCriteriaParsed
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityIdentifierParsed
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityScriptVariantParsed
import org.eclipse.tractusx.bpdm.pool.model.LegalEntityState
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneOffset

/**
 * Pure translation from the parsed legal-entity header domain model to JPA entities. Builds only the legal-entity header —
 * the legal address is built/applied by [AddressEntityMapper] / the address services. Carries no business logic and no
 * side effects: `currentness` and `numberOfSharingMembers` are supplied by the caller (the service stamps the current
 * timestamp and supplies the sharing-member count), and confidence translation is delegated to [AddressEntityMapper] (it
 * is not address-specific). The granular sub-builders are reused by the update path, which assigns their outputs onto an
 * existing managed entity.
 *
 * [toEntity] leaves `legalAddress` unset (it is `lateinit`): the create service assigns it after persisting the legal
 * address, mirroring the cyclic legal-entity↔legal-address relationship.
 */
@Component
class LegalEntityEntityMapper(
    private val addressEntityMapper: AddressEntityMapper
) {

    fun toEntity(bpn: String, header: LegalEntityHeaderParsed, currentness: Instant, numberOfSharingMembers: Int): LegalEntityDb {
        val entity = LegalEntityDb(
            bpn = bpn,
            legalName = toLegalName(header),
            legalForm = header.legalForm,
            currentness = currentness,
            confidenceCriteria = toConfidence(header.confidenceCriteria, numberOfSharingMembers),
            isCatenaXMemberData = header.isParticipantData,
            scriptVariants = toScriptVariants(header.scriptVariants).toMutableList()
        )
        entity.identifiers.addAll(toIdentifiers(header.identifiers, entity))
        entity.states.addAll(toStates(header.states, entity))
        return entity
    }

    fun toLegalName(header: LegalEntityHeaderParsed): NameDb =
        NameDb(value = header.legalName, shortName = header.legalShortName)

    fun toConfidence(parsed: ConfidenceCriteriaParsed, numberOfSharingMembers: Int): ConfidenceCriteriaDb =
        addressEntityMapper.toConfidence(parsed, numberOfSharingMembers)

    fun toIdentifiers(parsed: List<LegalEntityIdentifierParsed>, parent: LegalEntityDb): List<LegalEntityIdentifierDb> =
        parsed.map { LegalEntityIdentifierDb(value = it.value, type = it.type, issuingBody = it.issuingBody, legalEntity = parent) }

    fun toStates(parsed: List<LegalEntityState>, parent: LegalEntityDb): List<LegalEntityStateDb> =
        parsed.map { LegalEntityStateDb(validFrom = it.validFrom?.toLocalDateTime(), validTo = it.validTo?.toLocalDateTime(), type = it.type, legalEntity = parent) }

    fun toScriptVariants(parsed: List<LegalEntityScriptVariantParsed>): List<LegalEntityScriptVariantDb> =
        parsed.map { LegalEntityScriptVariantDb(scriptCode = it.scriptCode, legalName = it.legalName, shortName = it.shortName) }

    private fun Instant.toLocalDateTime() = atZone(ZoneOffset.UTC).toLocalDateTime()
}
