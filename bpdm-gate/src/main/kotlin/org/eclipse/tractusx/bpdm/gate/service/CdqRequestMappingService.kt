package org.eclipse.tractusx.bpdm.gate.service

import org.eclipse.tractusx.bpdm.common.dto.*
import org.eclipse.tractusx.bpdm.common.dto.cdq.*
import org.eclipse.tractusx.bpdm.common.model.BusinessPartnerType
import org.eclipse.tractusx.bpdm.gate.config.BpnConfigProperties
import org.eclipse.tractusx.bpdm.gate.config.CdqConfigProperties
import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityDto
import org.springframework.stereotype.Service

@Service
class CdqRequestMappingService(
    private val bpnConfigProperties: BpnConfigProperties,
    private val cdqConfigProperties: CdqConfigProperties
) {
    fun toCdqModel(legalEntity: LegalEntityDto): BusinessPartnerCdq {
        return BusinessPartnerCdq(
            externalId = legalEntity.externalId,
            dataSource = cdqConfigProperties.datasource,
            identifiers = toIdentifiersCdq(legalEntity.identifiers, legalEntity.bpn),
            names = legalEntity.names.map { it.toCdqModel() },
            legalForm = toLegalFormCdq(legalEntity.legalForm),
            status = legalEntity.status?.toCdqModel(),
            profile = toPartnerProfileCdq(legalEntity.profileClassifications),
            types = legalEntity.types.map { it.toCdqModel() },
            bankAccounts = legalEntity.bankAccounts.map { it.toCdqModel() }
        )
    }

    private fun BankAccountDto.toCdqModel(): BankAccountCdq {
        return BankAccountCdq(
            internationalBankAccountIdentifier = internationalBankAccountIdentifier,
            internationalBankIdentifier = internationalBankIdentifier,
            nationalBankAccountIdentifier = nationalBankAccountIdentifier,
            nationalBankIdentifier = nationalBankIdentifier
        )
    }

    private fun BusinessPartnerType.toCdqModel(): TypeKeyNameUrlCdq {
        return TypeKeyNameUrlCdq(name)
    }

    private fun toPartnerProfileCdq(profileClassifications: Collection<ClassificationDto>): PartnerProfileCdq? {
        if (profileClassifications.isEmpty()) {
            return null
        }
        return PartnerProfileCdq(classifications = profileClassifications.map { it.toCdqModel() })
    }

    private fun ClassificationDto.toCdqModel(): ClassificationCdq {
        return ClassificationCdq(
            value = value,
            code = code,
            type = if (type != null) TypeKeyNameUrlCdq(type!!.name) else null
        )
    }

    private fun BusinessStatusDto.toCdqModel(): BusinessPartnerStatusCdq {
        return BusinessPartnerStatusCdq(
            type = TypeKeyNameUrlCdq(type.name),
            officialDenotation = officialDenotation,
            validFrom = validFrom,
            validUntil = validUntil
        )
    }

    private fun toLegalFormCdq(technicalKey: String?) = if (technicalKey != null) LegalFormCdq(technicalKey = technicalKey) else null

    private fun NameDto.toCdqModel(): NameCdq {
        return NameCdq(
            value = value,
            shortName = shortName,
            type = TypeKeyNameUrlCdq(type.name)
        )
    }

    private fun IdentifierDto.toCdqModel(): IdentifierCdq {
        return IdentifierCdq(
            type = TypeKeyNameUrlCdq(type),
            value = value,
            issuingBody = TypeKeyNameUrlCdq(issuingBody),
            status = TypeKeyNameCdq(status)
        )
    }

    private fun toIdentifiersCdq(identifiers: Collection<IdentifierDto>, bpn: String?): Collection<IdentifierCdq> {
        var identifiersCdq = identifiers.map { it.toCdqModel() }
        if (bpn != null) {
            identifiersCdq = identifiersCdq.plus(createBpnIdentifierCdq(bpn))
        }
        return identifiersCdq
    }

    private fun createBpnIdentifierCdq(bpn: String): IdentifierCdq {
        return IdentifierCdq(
            type = TypeKeyNameUrlCdq(bpnConfigProperties.id, bpnConfigProperties.name),
            value = bpn,
            issuingBody = TypeKeyNameUrlCdq(bpnConfigProperties.agencyKey, bpnConfigProperties.agencyName)
        )
    }
}