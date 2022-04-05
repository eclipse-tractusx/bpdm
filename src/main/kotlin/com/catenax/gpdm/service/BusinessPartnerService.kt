package com.catenax.gpdm.service

import com.catenax.gpdm.dto.request.BusinessPartnerRequest
import com.catenax.gpdm.dto.request.BusinessPartnerSearchRequest
import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.entity.IdentifierStatus
import com.catenax.gpdm.entity.IdentifierType
import com.catenax.gpdm.exception.BpdmNotFoundException
import com.catenax.gpdm.repository.elastic.CustomSearchRepository
import com.catenax.gpdm.repository.entity.BusinessPartnerRepository
import com.catenax.gpdm.repository.entity.IdentifierStatusRepository
import com.catenax.gpdm.repository.entity.IdentifierTypeRepository
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BusinessPartnerService(
    val requestConversionService: RequestConversionService,
    val persistenceService: PersistenceService,
    val businessPartnerRepository: BusinessPartnerRepository,
    val identifierTypeRepository: IdentifierTypeRepository,
    val identifierStatusRepository: IdentifierStatusRepository,
    val customSearchRepository: CustomSearchRepository
) {

    @Transactional
    fun findPartner(bpn: String): BusinessPartnerResponse {
        val bp = businessPartnerRepository.findByBpn(bpn) ?: throw BpdmNotFoundException("Business Partner", bpn)
        return bp.toDto()
    }

    @Transactional
    fun findPartners(searchRequest: BusinessPartnerSearchRequest, pageRequest: PageRequest): PageResponse<BusinessPartnerResponse> {
        val searchResultPage = customSearchRepository.findBySearchRequest(searchRequest, pageRequest)
        val bpPage = PageImpl(businessPartnerRepository.findDistinctByBpnIn(searchResultPage.content.map { it.bpn }).toList(),
            searchResultPage.pageable,
            searchResultPage.totalElements)

        return bpPage.toDto( bpPage.content.map { it.toDto() } )
    }

    @Transactional
    fun findPartnerByIdentifier(identifierType: String, identifierValue: String): BusinessPartnerResponse {
        val type = identifierTypeRepository.findByTechnicalKey(identifierType) ?: throw BpdmNotFoundException(IdentifierType::class, identifierType)
        return businessPartnerRepository.findByIdentifierTypeAndValue(type, identifierValue)?.toDto()
            ?: throw BpdmNotFoundException("Identifier Value", identifierValue)
    }

    @Transactional
    fun findPartnersByIdentifier(identifierType: String, identifierValues: Collection<String>): Collection<BusinessPartnerResponse> {
       return businessPartnerRepository.findByIdentifierTypeAndValues(identifierType, identifierValues).map { it.toDto() }
    }

    @Transactional
    fun findPartnersByIdentifier(typeKey: String, statusKey: String) :Collection<BusinessPartnerResponse>{
        val type = identifierTypeRepository.findByTechnicalKey(typeKey) ?: throw BpdmNotFoundException(IdentifierType::class, typeKey)
        val status = identifierStatusRepository.findByTechnicalKey(statusKey) ?: throw BpdmNotFoundException(IdentifierStatus::class, statusKey)

        return businessPartnerRepository.findByIdentifierTypeAndStatus(type, status).map { it.toDto() }
    }

    @Transactional
    fun createPartners(bpDtos: Collection<BusinessPartnerRequest>): Collection<BusinessPartnerResponse>{
        val bpEntities = requestConversionService.buildBusinessPartners(bpDtos)
        persistenceService.saveAll(bpEntities)
        return bpEntities.map { it.toDto() }
    }


}