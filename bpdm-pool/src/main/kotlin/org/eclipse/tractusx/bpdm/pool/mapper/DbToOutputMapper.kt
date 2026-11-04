/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

import org.eclipse.tractusx.bpdm.pool.entity.BpnIdentifierMatchDb
import org.eclipse.tractusx.bpdm.pool.entity.BpnRequestIdentifierMappingDb
import org.eclipse.tractusx.bpdm.pool.entity.CountryDb
import org.eclipse.tractusx.bpdm.pool.entity.FieldQualityRuleDb
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalFormDb
import org.eclipse.tractusx.bpdm.pool.entity.ReasonCodeDb
import org.eclipse.tractusx.bpdm.pool.entity.RegionDb
import org.eclipse.tractusx.bpdm.pool.entity.ScriptCodeDb
import org.eclipse.tractusx.bpdm.pool.model.output.bpn.BpnIdentifierMatchRecord
import org.eclipse.tractusx.bpdm.pool.model.output.bpn.BpnRequestIdentifierMatchRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.AdministrativeAreaRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.CountryRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.FieldQualityRuleRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.IdentifierTypeDetailRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.IdentifierTypeRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.LegalFormRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.ReasonCodeRecord
import org.eclipse.tractusx.bpdm.pool.model.output.metadata.ScriptCodeRecord
import org.springframework.stereotype.Component

/**
 * Maps persisted entities to the output records a use case hands back.
 */
@Component
class DbToOutputMapper {

    /**
     * Returns the output record for [entity].
     */
    fun toRecord(entity: ReasonCodeDb): ReasonCodeRecord =
        ReasonCodeRecord(
            technicalKey = entity.technicalKey,
            description = entity.description
        )

    /**
     * Returns the output record for [entity].
     */
    fun toRecord(entity: ScriptCodeDb): ScriptCodeRecord =
        ScriptCodeRecord(
            technicalKey = entity.technicalKey,
            description = entity.description
        )

    /**
     * Returns the output record for [entity].
     */
    fun toRecord(entity: CountryDb): CountryRecord =
        CountryRecord(
            countryCode = entity.countryCode,
            name = entity.name,
            description = entity.description
        )

    /**
     * Returns the output record for [entity].
     */
    fun toRecord(entity: RegionDb): AdministrativeAreaRecord =
        AdministrativeAreaRecord(
            countryCode = entity.country.countryCode,
            code = entity.regionCode,
            name = entity.regionName
        )

    /**
     * Returns the output record for [entity].
     */
    fun toRecord(entity: LegalFormDb): LegalFormRecord =
        LegalFormRecord(
            technicalKey = entity.technicalKey,
            name = entity.name,
            transliteratedName = entity.transliteratedName,
            abbreviation = entity.abbreviation,
            transliteratedAbbreviations = entity.transliteratedAbbreviations,
            countryCode = entity.country?.countryCode,
            languageCode = entity.languageCode,
            administrativeAreaCode = entity.administrativeArea?.regionCode,
            isActive = entity.isActive
        )

    /**
     * Returns the output record for [entity].
     */
    fun toRecord(entity: IdentifierTypeDb): IdentifierTypeRecord =
        IdentifierTypeRecord(
            technicalKey = entity.technicalKey,
            businessPartnerType = entity.businessPartnerType,
            name = entity.name,
            abbreviation = entity.abbreviation,
            transliteratedName = entity.transliteratedName,
            transliteratedAbbreviation = entity.transliteratedAbbreviation,
            format = entity.format,
            categories = entity.categories.toSet(),
            details = entity.details.map { IdentifierTypeDetailRecord(countryCode = it.country?.countryCode, mandatory = it.mandatory) }
        )

    /**
     * Returns the output record for [entity] as it applies in [country].
     */
    fun toRecord(entity: FieldQualityRuleDb, country: CountryDb): FieldQualityRuleRecord =
        FieldQualityRuleRecord(
            fieldPath = entity.fieldPath,
            schemaName = entity.schemaName,
            countryCode = country.countryCode,
            qualityLevel = entity.qualityLevel
        )

    /**
     * Returns the output record for [entity].
     */
    fun toRecord(entity: BpnIdentifierMatchDb): BpnIdentifierMatchRecord =
        BpnIdentifierMatchRecord(
            identifierValue = entity.identifierValue,
            bpn = entity.bpn
        )

    /**
     * Returns the output record for [entity].
     */
    fun toRecord(entity: BpnRequestIdentifierMappingDb): BpnRequestIdentifierMatchRecord =
        BpnRequestIdentifierMatchRecord(
            requestIdentifier = entity.requestIdentifier,
            bpn = entity.bpn
        )
}
