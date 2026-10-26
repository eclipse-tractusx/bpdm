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
import org.eclipse.tractusx.bpdm.gate.api.model.AddressScriptVariantDto
import org.eclipse.tractusx.bpdm.gate.api.model.AlternativeAddressScriptVariantDto
import org.eclipse.tractusx.bpdm.gate.api.model.BusinessPartnerScriptVariantDto
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityScriptVariantDto
import org.eclipse.tractusx.bpdm.gate.api.model.PhysicalAddressScriptVariantDto
import org.eclipse.tractusx.bpdm.gate.api.model.SiteScriptVariantDto
import org.eclipse.tractusx.bpdm.gate.api.model.StreetScriptVariantDto
import org.eclipse.tractusx.bpdm.gate.entity.generic.BusinessPartnerDb
import org.eclipse.tractusx.bpdm.gate.mapper.record.OutputUpsertDataMapper
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.AddressAlignmentParsed
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.BusinessPartnerOutputParsed
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.LegalEntityGoldenRecordRequest
import org.eclipse.tractusx.bpdm.gate.model.goldenrecord.SiteGoldenRecordRequest
import org.eclipse.tractusx.bpdm.gate.model.upsert.output.OutputUpsertData
import org.eclipse.tractusx.bpdm.gate.model.value.BusinessPartnerValues
import org.springframework.stereotype.Component

/**
 * Maps a stored business partner output and the golden records it aligns with onto the output data it is rewritten to.
 */
@Component
class BusinessPartnerOutputParsedMapper(
    private val upsertDataMapper: OutputUpsertDataMapper
) {

    /**
     * Returns [output] with the golden records of [legalEntity], [site] and [address] laid over its stored data, leaving
     * a component that is null as stored.
     */
    fun toParsed(
        output: BusinessPartnerDb,
        legalEntity: LegalEntityGoldenRecordRequest?,
        site: SiteGoldenRecordRequest?,
        address: AddressAlignmentParsed?
    ): BusinessPartnerOutputParsed {
        val stored = upsertDataMapper.toUpsertData(output)
        val aligned = stored
            .let { data -> legalEntity?.let { withLegalEntity(data, it) } ?: data }
            .let { data -> site?.let { withSite(data, it) } ?: data }
            .let { data -> address?.let { withAddress(data, it) } ?: data }
        return BusinessPartnerOutputParsed(output, aligned)
    }

    private fun withLegalEntity(data: OutputUpsertData, legalEntity: LegalEntityGoldenRecordRequest): OutputUpsertData =
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
                variant.copy(legalEntity = LegalEntityScriptVariantDto(goldenRecord.legalName, goldenRecord.shortName))
            }
        )

    private fun withSite(data: OutputUpsertData, site: SiteGoldenRecordRequest): OutputUpsertData =
        data.copy(
            siteBpn = site.bpn,
            states = data.states.filterNot { it.businessPartnerType == BusinessPartnerType.SITE } + site.states,
            siteName = site.name,
            siteConfidence = site.confidence,
            siteGoldenRecordRelations = site.relations,
            siteUpdatedAt = site.updatedAt,
            scriptVariants = mergeScriptVariants(data.scriptVariants, site.scriptVariants, { it.scriptCode }) { variant, goldenRecord ->
                variant.copy(site = SiteScriptVariantDto(goldenRecord.name))
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
                    address = AddressScriptVariantDto(
                        name = goldenRecord.name,
                        physicalAddress = toScriptVariantDto(goldenRecord.physicalAddress),
                        alternativeAddress = goldenRecord.alternativeAddressCity?.let { AlternativeAddressScriptVariantDto(it) }
                    )
                )
            }
        )
    }

    private fun <G> mergeScriptVariants(
        variants: List<BusinessPartnerScriptVariantDto>,
        goldenRecordVariants: List<G>,
        scriptCodeOf: (G) -> String,
        apply: (BusinessPartnerScriptVariantDto, G) -> BusinessPartnerScriptVariantDto
    ): List<BusinessPartnerScriptVariantDto> {
        val goldenRecordVariantByCode = goldenRecordVariants.associateBy(scriptCodeOf)
        val storedCodes = variants.map { it.scriptCode }.toSet()
        val updated = variants.map { variant -> goldenRecordVariantByCode[variant.scriptCode]?.let { apply(variant, it) } ?: variant }
        val added = goldenRecordVariantByCode.filterKeys { it !in storedCodes }
            .map { (scriptCode, goldenRecordVariant) -> apply(BusinessPartnerScriptVariantDto(scriptCode), goldenRecordVariant) }
        return updated + added
    }

    private fun toScriptVariantDto(physicalAddress: BusinessPartnerValues.PhysicalAddressScriptVariant) =
        with(physicalAddress) {
            PhysicalAddressScriptVariantDto(
                city = city,
                district = district,
                street = with(street) { StreetScriptVariantDto(name, direction, namePrefix, additionalNamePrefix, nameSuffix, additionalNameSuffix) },
                industrialZone = industrialZone,
                building = building,
                floor = floor,
                door = door
            )
        }
}
