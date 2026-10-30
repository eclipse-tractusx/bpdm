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

package org.eclipse.tractusx.bpdm.gate.mapper.parsed

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.gate.entity.SharingStateDb
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.model.parsed.goldenrecord.AddressAlignmentParsed
import org.eclipse.tractusx.bpdm.gate.model.parsed.goldenrecord.BusinessPartnerOutputParsed
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.LegalEntityGoldenRecordRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.goldenrecord.SiteGoldenRecordRaw
import org.eclipse.tractusx.bpdm.gate.model.raw.value.BusinessPartnerValuesRaw
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.OutputUpsertData
import org.springframework.stereotype.Component

/**
 * Maps business partner output data and the golden records it aligns with onto the output data it is written as.
 */
@Component
class BusinessPartnerOutputParsedMapper {

    /**
     * Returns the output of [sharingState] with the golden records of [legalEntity], [site] and [address] laid over
     * [base], leaving a component that is null as [base] holds it.
     */
    fun toParsed(
        sharingState: SharingStateDb,
        storedOutput: BusinessPartnerDb?,
        base: OutputUpsertData,
        legalEntity: LegalEntityGoldenRecordRaw?,
        site: SiteGoldenRecordRaw?,
        address: AddressAlignmentParsed?
    ): BusinessPartnerOutputParsed {
        val aligned = base
            .let { data -> legalEntity?.let { withLegalEntity(data, it) } ?: data }
            .let { data -> site?.let { withSite(data, it) } ?: data }
            .let { data -> address?.let { withAddress(data, it) } ?: data }
        return BusinessPartnerOutputParsed(sharingState, storedOutput, aligned)
    }

    private fun withLegalEntity(data: OutputUpsertData, legalEntity: LegalEntityGoldenRecordRaw): OutputUpsertData =
        data.copy(
            identifiers = data.identifiers.filterNot { it.businessPartnerType == BusinessPartnerType.LEGAL_ENTITY } + legalEntity.identifiers,
            states = data.states.filterNot { it.businessPartnerType == BusinessPartnerType.LEGAL_ENTITY } + legalEntity.states,
            legalName = legalEntity.legalName,
            legalForm = legalEntity.legalForm,
            shortName = legalEntity.shortName,
            legalEntityConfidence = legalEntity.confidence,
            legalEntityGoldenRecordRelations = legalEntity.relations,
            ownershipUltimate = legalEntity.ownershipUltimate,
            ultimateOwnerBpnl = legalEntity.ultimateOwnerBpnl,
            legalEntityUpdatedAt = legalEntity.updatedAt,
            scriptVariants = mergeScriptVariants(data.scriptVariants, legalEntity.scriptVariants, { it.scriptCode }) { variant, goldenRecord ->
                variant.copy(legalName = goldenRecord.legalName, shortName = goldenRecord.shortName)
            }
        )

    private fun withSite(data: OutputUpsertData, site: SiteGoldenRecordRaw): OutputUpsertData =
        data.copy(
            siteBpn = site.bpn,
            states = data.states.filterNot { it.businessPartnerType == BusinessPartnerType.SITE } + site.states,
            siteName = site.name,
            siteConfidence = site.confidence,
            siteGoldenRecordRelations = site.relations,
            siteUpdatedAt = site.updatedAt,
            scriptVariants = mergeScriptVariants(data.scriptVariants, site.scriptVariants, { it.scriptCode }) { variant, goldenRecord ->
                variant.copy(siteName = goldenRecord.name)
            }
        )

    private fun withAddress(data: OutputUpsertData, alignment: AddressAlignmentParsed): OutputUpsertData {
        val address = alignment.address
        return data.copy(
            identifiers = data.identifiers.filterNot { it.businessPartnerType == BusinessPartnerType.ADDRESS } + address.identifiers,
            states = data.states.filterNot { it.businessPartnerType == BusinessPartnerType.ADDRESS } + address.states,
            addressName = address.name,
            addressType = alignment.addressType,
            physicalPostalAddress = address.physicalPostalAddress,
            alternativePostalAddress = address.alternativePostalAddress,
            addressConfidence = address.confidence,
            addressGoldenRecordRelations = address.relations,
            additionalSites = alignment.additionalSites,
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
        variants: List<BusinessPartnerValuesRaw.ScriptVariant>,
        goldenRecordVariants: List<G>,
        scriptCodeOf: (G) -> String,
        apply: (BusinessPartnerValuesRaw.ScriptVariant, G) -> BusinessPartnerValuesRaw.ScriptVariant
    ): List<BusinessPartnerValuesRaw.ScriptVariant> {
        val goldenRecordVariantByCode = goldenRecordVariants.associateBy(scriptCodeOf)
        val storedCodes = variants.map { it.scriptCode }.toSet()
        val updated = variants.map { variant -> goldenRecordVariantByCode[variant.scriptCode]?.let { apply(variant, it) } ?: variant }
        val added = goldenRecordVariantByCode.filterKeys { it !in storedCodes }
            .map { (scriptCode, goldenRecordVariant) -> apply(emptyScriptVariant(scriptCode), goldenRecordVariant) }
        return updated + added
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
