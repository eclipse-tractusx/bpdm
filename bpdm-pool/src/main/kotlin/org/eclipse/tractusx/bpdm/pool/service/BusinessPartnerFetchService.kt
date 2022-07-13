package org.eclipse.tractusx.bpdm.pool.service

import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.pool.dto.response.BpnIdentifierMappingResponse
import org.eclipse.tractusx.bpdm.pool.dto.response.BusinessPartnerResponse
import org.eclipse.tractusx.bpdm.pool.entity.BusinessPartner
import org.eclipse.tractusx.bpdm.pool.entity.Identifier
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierType
import org.eclipse.tractusx.bpdm.pool.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for fetching business partner records from the database
 */
@Service
class BusinessPartnerFetchService(
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val identifierTypeRepository: IdentifierTypeRepository,
    private val identifierRepository: IdentifierRepository,
    private val legalFormRepository: LegalFormRepository,
    private val bankAccountRepository: BankAccountRepository
) {

    /**
     * Fetch a business partner by [bpn] and return as [BusinessPartnerResponse]
     */
    @Transactional
    fun findPartner(bpn: String): BusinessPartnerResponse {
        val bp = businessPartnerRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Business Partner", bpn)
        return bp.toDto()
    }

    /**
     * Fetch business partners by BPN in [bpns]
     */
    @Transactional
    fun fetchByBpns(bpns: Collection<String>): Set<BusinessPartner> {
        return fetchBusinessPartnerDependencies(businessPartnerRepository.findDistinctByBpnIn(bpns))
    }

    /**
     * Fetch a business partner by [identifierValue] of [identifierType] and return as [BusinessPartnerResponse]
     */
    @Transactional
    fun findPartnerByIdentifier(identifierType: String, identifierValue: String): BusinessPartnerResponse {
        val type = identifierTypeRepository.findByTechnicalKey(identifierType) ?: throw BpdmNotFoundException(IdentifierType::class, identifierType)
        return businessPartnerRepository.findByIdentifierTypeAndValue(type, identifierValue)?.toDto()
            ?: throw BpdmNotFoundException("Identifier Value", identifierValue)
    }

    /**
     * Fetch business partners by [values] of [identifierType]
     */
    @Transactional
    fun fetchByIdentifierValues(identifierType: String, values: Collection<String>): Set<BusinessPartner> {
        return fetchBusinessPartnerDependencies(businessPartnerRepository.findByIdentifierTypeAndValues(identifierType, values))
    }

    /**
     * Find bpn to identifier value mappings by [idValues] of [identifierType]
     */
    @Transactional
    fun findBpnsByIdentifiers(identifierType: String, idValues: Collection<String>): Set<BpnIdentifierMappingResponse> {
        val type = identifierTypeRepository.findByTechnicalKey(identifierType) ?: throw BpdmNotFoundException(IdentifierType::class, identifierType)
        return identifierRepository.findBpnsByIdentifierTypeAndValues(type, idValues)
    }

    private fun fetchBusinessPartnerDependencies(partners: Set<BusinessPartner>): Set<BusinessPartner> {

        businessPartnerRepository.joinIdentifiers(partners)
        businessPartnerRepository.joinNames(partners)
        businessPartnerRepository.joinStatuses(partners)
        businessPartnerRepository.joinClassifications(partners)
        businessPartnerRepository.joinBankAccounts(partners)
        businessPartnerRepository.joinRelations(partners)
        businessPartnerRepository.joinTypes(partners)
        businessPartnerRepository.joinRoles(partners)
        businessPartnerRepository.joinLegalForm(partners)

        // don't fetch sites/addresses since those are not needed when mapping to BusinessPartnerResponse

        val identifiers = partners.flatMap { it.identifiers }.toSet()
        fetchIdentifierDependencies(identifiers)

        val legalForms = partners.mapNotNull { it.legalForm }.toSet()
        legalFormRepository.joinCategories(legalForms)

        val bankAccounts = partners.flatMap { it.bankAccounts }.toSet()
        bankAccountRepository.joinTrustScores(bankAccounts)

        return partners
    }

    private fun fetchIdentifierDependencies(identifiers: Set<Identifier>): Set<Identifier> {
        identifierRepository.joinType(identifiers)
        identifierRepository.joinStatus(identifiers)
        identifierRepository.joinIssuingBody(identifiers)

        return identifiers
    }


}