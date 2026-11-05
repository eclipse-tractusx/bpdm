/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.bpdm.gate.service.parser.goldenrecord

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.common.model.zipParseResults
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerOutputContentParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.businesspartner.BusinessPartnerOutputParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.goldenrecord.AddressAlignmentParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.goldenrecord.GoldenRecordAlignmentError
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.GoldenRecordsRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.LegalEntityGoldenRecordRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.SiteGoldenRecordRaw
import org.springframework.stereotype.Service

/**
 * Derives the output data business partner outputs take on from golden records.
 */
@Service
class GoldenRecordAlignmentParser(
    private val legalEntityParser: LegalEntityAlignmentParser,
    private val siteParser: SiteAlignmentParser,
    private val addressParser: AddressAlignmentParser
) {

    /**
     * Returns the output data of [sharingState] that [base] takes on from the golden records in [goldenRecords] it
     * references, keeping each component [base] holds more recently.
     */
    fun parse(
        sharingState: SharingStateDb,
        storedOutput: BusinessPartnerDb?,
        base: BusinessPartnerOutputContentParsed,
        goldenRecords: GoldenRecordsRaw
    ): ParseResult<BusinessPartnerOutputParsed, GoldenRecordAlignmentError> =
        zipParseResults(
            legalEntityParser.parse(sharingState, base, goldenRecords),
            siteParser.parse(sharingState, base, goldenRecords),
            addressParser.parse(sharingState, base, goldenRecords)
        ) { legalEntity, site, address ->
            val aligned = base
                .let { data -> legalEntity?.let { withLegalEntity(data, it) } ?: data }
                .let { data -> (site ?: address?.adoptedSite)?.let { withSite(data, it) } ?: data }
                .let { data -> address?.let { withAddress(data, it) } ?: data }
                .let { data -> withStoredScriptVariantNameParts(data, storedOutput) }
            BusinessPartnerOutputParsed(sharingState, storedOutput, aligned)
        }

    /**
     * Restores the name parts of [storedOutput]'s script variants on every variant of [data] that lacks them, since
     * golden records do not carry uncategorized name parts.
     */
    private fun withStoredScriptVariantNameParts(data: BusinessPartnerOutputContentParsed, storedOutput: BusinessPartnerDb?): BusinessPartnerOutputContentParsed {
        val storedNamePartsByCode = storedOutput?.scriptVariants.orEmpty().associate { it.scriptCode to it.nameParts.toList() }
        if (storedNamePartsByCode.isEmpty()) return data

        return data.copy(
            scriptVariants = data.scriptVariants.mapTo(linkedSetOf()) { variant ->
                if (variant.nameParts.isEmpty()) variant.copy(nameParts = storedNamePartsByCode[variant.scriptCode].orEmpty()) else variant
            }
        )
    }

    private fun withLegalEntity(data: BusinessPartnerOutputContentParsed, legalEntity: LegalEntityGoldenRecordRaw): BusinessPartnerOutputContentParsed =
        data.copy(
            identifiers = data.identifiers.filterNot { it.businessPartnerType == BusinessPartnerType.LEGAL_ENTITY }.toSet() + legalEntity.identifiers,
            states = data.states.filterNot { it.businessPartnerType == BusinessPartnerType.LEGAL_ENTITY }.toSet() + legalEntity.states,
            legalName = legalEntity.legalName,
            legalForm = legalEntity.legalForm,
            shortName = legalEntity.shortName,
            legalEntityConfidence = legalEntity.confidence,
            legalEntityGoldenRecordRelations = legalEntity.relations.toSet(),
            ownershipUltimate = legalEntity.ownershipUltimate,
            ultimateOwnerBpnl = legalEntity.ultimateOwnerBpnl,
            legalEntityUpdatedAt = legalEntity.updatedAt,
            scriptVariants = mergeScriptVariants(data.scriptVariants, legalEntity.scriptVariants, { it.scriptCode }) { variant, goldenRecord ->
                variant.copy(legalName = goldenRecord.legalName, shortName = goldenRecord.shortName)
            }
        )

    private fun withSite(data: BusinessPartnerOutputContentParsed, site: SiteGoldenRecordRaw): BusinessPartnerOutputContentParsed =
        data.copy(
            siteBpn = site.bpn,
            states = data.states.filterNot { it.businessPartnerType == BusinessPartnerType.SITE }.toSet() + site.states,
            siteName = site.name,
            siteConfidence = site.confidence,
            siteGoldenRecordRelations = site.relations.toSet(),
            siteUpdatedAt = site.updatedAt,
            scriptVariants = mergeScriptVariants(data.scriptVariants, site.scriptVariants, { it.scriptCode }) { variant, goldenRecord ->
                variant.copy(siteName = goldenRecord.name)
            }
        )

    private fun withAddress(data: BusinessPartnerOutputContentParsed, alignment: AddressAlignmentParsed): BusinessPartnerOutputContentParsed {
        val address = alignment.address
        return data.copy(
            identifiers = data.identifiers.filterNot { it.businessPartnerType == BusinessPartnerType.ADDRESS }.toSet() + address.identifiers,
            states = data.states.filterNot { it.businessPartnerType == BusinessPartnerType.ADDRESS }.toSet() + address.states,
            addressName = address.name,
            addressType = alignment.addressType,
            physicalPostalAddress = address.physicalPostalAddress,
            alternativePostalAddress = address.alternativePostalAddress,
            addressConfidence = address.confidence,
            addressGoldenRecordRelations = address.relations.toSet(),
            additionalSites = alignment.additionalSites.toSet(),
            addressUpdatedAt = address.updatedAt,
            scriptVariants = mergeScriptVariants(data.scriptVariants, address.scriptVariants, { it.scriptCode }) { variant, goldenRecord ->
                variant.copy(
                    addressName = goldenRecord.name,
                    physicalAddress = goldenRecord.physicalAddress,
                    alternativeAddressCity = goldenRecord.alternativeAddressCity
                )
            }
        )
    }

    private fun <G> mergeScriptVariants(
        variants: Set<BusinessPartnerValuesRaw.ScriptVariant>,
        goldenRecordVariants: List<G>,
        scriptCodeOf: (G) -> String,
        apply: (BusinessPartnerValuesRaw.ScriptVariant, G) -> BusinessPartnerValuesRaw.ScriptVariant
    ): Set<BusinessPartnerValuesRaw.ScriptVariant> {
        val goldenRecordVariantByCode = goldenRecordVariants.associateBy(scriptCodeOf)
        val storedCodes = variants.map { it.scriptCode }.toSet()
        val updated = variants.map { variant -> goldenRecordVariantByCode[variant.scriptCode]?.let { apply(variant, it) } ?: variant }
        val added = goldenRecordVariantByCode.filterKeys { it !in storedCodes }
            .map { (scriptCode, goldenRecordVariant) -> apply(emptyScriptVariant(scriptCode), goldenRecordVariant) }
        return (updated + added).toSet()
    }

    private fun emptyScriptVariant(scriptCode: String) =
        BusinessPartnerValuesRaw.ScriptVariant(
            scriptCode = scriptCode,
            nameParts = emptyList(),
            legalName = null,
            shortName = null,
            siteName = null,
            addressName = null,
            physicalAddress = BusinessPartnerValuesRaw.PhysicalAddressScriptVariant(
                city = null,
                district = null,
                street = BusinessPartnerValuesRaw.StreetScriptVariant(null, null, null, null, null, null),
                industrialZone = null,
                building = null,
                floor = null,
                door = null
            ),
            alternativeAddressCity = null
        )
}
