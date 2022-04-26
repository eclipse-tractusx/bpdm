package com.catenax.gpdm.service

import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import com.catenax.gpdm.entity.Address
import com.catenax.gpdm.entity.BusinessPartner
import com.catenax.gpdm.entity.Identifier
import com.catenax.gpdm.entity.IdentifierType
import com.catenax.gpdm.exception.BpdmNotFoundException
import com.catenax.gpdm.repository.BusinessPartnerRepository
import com.catenax.gpdm.repository.IdentifierRepository
import com.catenax.gpdm.repository.IdentifierTypeRepository
import com.catenax.gpdm.repository.entity.AddressRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for fetching business partner records from the database
 */
@Service
class BusinessPartnerFetchService(
    private val businessPartnerRepository: BusinessPartnerRepository,
    private val identifierTypeRepository: IdentifierTypeRepository,
    private val addressRepository: AddressRepository,
    private val identifierRepository: IdentifierRepository
        ){

    /**
     * Fetch a business partner by [bpn] and return as [BusinessPartnerResponse]
     */
    @Transactional
    fun findPartner(bpn: String): BusinessPartnerResponse {
        val bp = businessPartnerRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Business Partner", bpn)
        return bp.toDto()
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
    fun fetchByIdentifierValues(identifierType: String, values: Collection<String>): Set<BusinessPartner>{
        return fetchBusinessPartnerDependencies(businessPartnerRepository.findByIdentifierTypeAndValues(identifierType, values))
    }

    private fun fetchBusinessPartnerDependencies(partners: Set<BusinessPartner>): Set<BusinessPartner>{

        businessPartnerRepository.joinIdentifiers(partners)
        businessPartnerRepository.joinNames(partners)
        businessPartnerRepository.joinAddresses(partners)
        businessPartnerRepository.joinSatuses(partners)
        businessPartnerRepository.joinClassifications(partners)
        businessPartnerRepository.joinBankAccounts(partners)
        businessPartnerRepository.joinRelations(partners)
        businessPartnerRepository.joinTypes(partners)
        businessPartnerRepository.joinRoles(partners)
        businessPartnerRepository.joinLegalForm(partners)

        val identifiers = partners.flatMap { it.identifiers }.toSet()
        fetchIdentifierDependencies(identifiers)

        val addresses = partners.flatMap { it.addresses }.toSet()
        fetchAddressDependencies(addresses)

        return partners
    }

    private fun fetchAddressDependencies(addresses: Set<Address>): Set<Address>{
        addressRepository.joinAdminAreas(addresses)
        addressRepository.joinPostCodes(addresses)
        addressRepository.joinLocalities(addresses)
        addressRepository.joinPremises(addresses)
        addressRepository.joinPostalDeliveryPoints(addresses)
        addressRepository.joinThoroughfares(addresses)
        addressRepository.joinVersion(addresses)

        return addresses
    }

    private fun fetchIdentifierDependencies(identifiers: Set<Identifier>): Set<Identifier>{
        identifierRepository.joinType(identifiers)
        identifierRepository.joinStatus(identifiers)
        identifierRepository.joinIssuingBody(identifiers)

        return identifiers
    }


}