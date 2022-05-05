package com.catenax.gpdm.service

import com.catenax.gpdm.dto.request.LegalFormRequest
import com.catenax.gpdm.dto.response.LegalFormResponse
import com.catenax.gpdm.dto.response.PageResponse
import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.*
import com.catenax.gpdm.exception.BpdmAlreadyExists
import com.catenax.gpdm.repository.*
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for fetching and creating metadata entities
 */
@Service
class MetadataService(
    val identifierTypeRepository: IdentifierTypeRepository,
    val issuingBodyRepository: IssuingBodyRepository,
    val legalFormRepository: LegalFormRepository,
    val legalFormCategoryRepository: LegalFormCategoryRepository,
    val identifierStatusRepository: IdentifierStatusRepository
) {

    @Transactional
    fun getOrCreateIdentifierType(type: TypeKeyNameUrlDto<String>): TypeKeyNameUrlDto<String>{
        val newIdentifier = identifierTypeRepository.findByTechnicalKey(type.technicalKey)
            ?.let { throw BpdmAlreadyExists(IdentifierType::class.simpleName!!, type.technicalKey) }
            ?: IdentifierType(type.name, type.url, type.technicalKey)

        return identifierTypeRepository.save(newIdentifier).toDto()
    }

    fun getIdentifierTypes(pageRequest: Pageable): PageResponse<TypeKeyNameUrlDto<String>> {
        val page = identifierTypeRepository.findAll(pageRequest)
        return page.toDto( page.content.map { it.toDto() } )
    }

    @Transactional
    fun getOrCreateIdentifierStatus(status: TypeKeyNameDto<String>): TypeKeyNameDto<String>{
        val newStatus = identifierStatusRepository.findByTechnicalKey(status.technicalKey)
            ?.let { throw BpdmAlreadyExists(IdentifierStatus::class.simpleName!!, status.technicalKey) }
            ?: IdentifierStatus(status.name, status.technicalKey)

        return identifierStatusRepository.save(newStatus).toDto()
    }

    fun getIdentifierStati(pageRequest: Pageable): PageResponse<TypeKeyNameDto<String>> {
        val page = identifierStatusRepository.findAll(pageRequest)
        return page.toDto( page.content.map { it.toDto() } )
    }

    @Transactional
    fun getOrCreateIssuingBody(type: TypeKeyNameUrlDto<String>): TypeKeyNameUrlDto<String>{
        val newIssuingBody = issuingBodyRepository.findByTechnicalKey(type.technicalKey)
            ?.let { throw BpdmAlreadyExists(IssuingBody::class.simpleName!!, type.technicalKey) }
            ?: IssuingBody(type.name, type.url, type.technicalKey)

        return issuingBodyRepository.save(newIssuingBody).toDto()
    }

    fun getIssuingBodies(pageRequest: Pageable): PageResponse<TypeKeyNameUrlDto<String>> {
        val page = issuingBodyRepository.findAll(pageRequest)
        return page.toDto( page.content.map { it.toDto() } )
    }

    @Transactional
    fun getOrCreateLegalForm(request: LegalFormRequest): LegalFormResponse{
        val legalForm = legalFormRepository.findByTechnicalKey(request.technicalKey)
            ?.let { throw BpdmAlreadyExists(LegalForm::class.simpleName!!, request.name) }
            ?: buildLegalForm(request)

        legalFormCategoryRepository.saveAll(legalForm.categories)
        return legalFormRepository.save(legalForm).toDto()
    }

    fun getLegalForms(pageRequest: Pageable): PageResponse<LegalFormResponse> {
        val page = legalFormRepository.findAll(pageRequest)
        return page.toDto( page.content.map { it.toDto() } )
    }

    private fun buildLegalForm(dto: LegalFormRequest): LegalForm{
        val categories = dto.category.map { LegalFormCategory(it.name, it.url) }.toSet()
        return LegalForm(dto.name, dto.url, dto.language, dto.mainAbbreviation, categories, dto.technicalKey)
    }

}