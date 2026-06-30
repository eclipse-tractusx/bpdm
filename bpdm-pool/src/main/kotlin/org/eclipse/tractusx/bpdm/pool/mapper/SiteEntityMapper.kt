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
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteScriptVariantDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteStateDb
import org.eclipse.tractusx.bpdm.pool.model.ConfidenceCriteriaParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteCreateParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteHeaderParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteScriptVariantParsed
import org.eclipse.tractusx.bpdm.pool.model.SiteState
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneOffset

/**
 * Pure translation from the parsed site header domain model to JPA entities. Builds only the site header — the main
 * address is built/applied by [AddressEntityMapper] / the address services. Carries no business logic and no side
 * effects: `numberOfSharingMembers` is supplied by the caller (1 for a new site, the preserved value for an update),
 * and confidence translation is delegated to [AddressEntityMapper] (it is not address-specific). The granular
 * sub-builders are reused by the update path, which assigns their outputs onto an existing managed entity.
 *
 * [toEntity] leaves `mainAddress` unset (it is `lateinit`): the create service assigns it after persisting the main
 * address, mirroring the cyclic site↔main-address relationship.
 */
@Component
class SiteEntityMapper(
    private val addressEntityMapper: AddressEntityMapper
) {

    fun toEntity(bpn: String, parsed: SiteCreateParsed, numberOfSharingMembers: Int): SiteDb =
        toEntity(bpn, parsed.legalEntity, parsed.content.header, numberOfSharingMembers)

    fun toEntity(bpn: String, legalEntity: LegalEntityDb, header: SiteHeaderParsed, numberOfSharingMembers: Int): SiteDb {
        val entity = SiteDb(
            bpn = bpn,
            name = header.name,
            confidenceCriteria = toConfidence(header.confidenceCriteria, numberOfSharingMembers),
            legalEntity = legalEntity,
            scriptVariants = toScriptVariants(header.scriptVariants).toMutableList()
        )
        entity.states.addAll(toStates(header.states, entity))
        return entity
    }

    fun toConfidence(parsed: ConfidenceCriteriaParsed, numberOfSharingMembers: Int): ConfidenceCriteriaDb =
        addressEntityMapper.toConfidence(parsed, numberOfSharingMembers)

    fun toStates(parsed: List<SiteState>, parent: SiteDb): List<SiteStateDb> =
        parsed.map { SiteStateDb(validFrom = it.validFrom?.toLocalDateTime(), validTo = it.validTo?.toLocalDateTime(), type = it.type, site = parent) }

    fun toScriptVariants(parsed: List<SiteScriptVariantParsed>): List<SiteScriptVariantDb> =
        parsed.map { SiteScriptVariantDb(scriptCode = it.scriptCode, name = it.name) }

    private fun Instant.toLocalDateTime() = atZone(ZoneOffset.UTC).toLocalDateTime()
}
