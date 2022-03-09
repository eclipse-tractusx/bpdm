package com.catenax.gpdm.component.elastic.mock.repository

import com.catenax.gpdm.dto.elastic.BusinessPartnerDoc
import com.catenax.gpdm.repository.elastic.BusinessPartnerDocRepository
import com.catenax.gpdm.service.DocumentMappingService
import com.catenax.gpdm.repository.entity.BusinessPartnerRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class BusinessPartnerDocRepositoryMock(
    val businessPartnerRepository: BusinessPartnerRepository,
    val documentMappingService: DocumentMappingService
): BusinessPartnerDocRepository {
    override fun <S : BusinessPartnerDoc?> save(entity: S): S {
        return entity
    }

    override fun <S : BusinessPartnerDoc?> saveAll(entities: MutableIterable<S>): MutableIterable<S> {
        return entities
    }

    override fun findById(id: String): Optional<BusinessPartnerDoc> {
       val bp = businessPartnerRepository.findByBpn(id)
        return if(bp != null) Optional.of(documentMappingService.toDocument(bp)) else Optional.empty()
    }

    override fun existsById(id: String): Boolean {
       return findById(id).isPresent
    }

    override fun findAll(sort: Sort): MutableIterable<BusinessPartnerDoc> {
       return businessPartnerRepository.findAll(sort).map { documentMappingService.toDocument(it) }.toMutableList()
    }

    override fun findAll(pageable: Pageable): Page<BusinessPartnerDoc> {
        return businessPartnerRepository.findAll(pageable).map { documentMappingService.toDocument(it) }
    }

    override fun findAll(): MutableIterable<BusinessPartnerDoc> {
        return businessPartnerRepository.findAll().map { documentMappingService.toDocument(it) }.toMutableList()
    }

    override fun findAllById(ids: MutableIterable<String>): MutableIterable<BusinessPartnerDoc> {
       return ids.mapNotNull { businessPartnerRepository.findByBpn(it) }.map { documentMappingService.toDocument(it) }.toMutableList()
    }

    override fun count(): Long {
        return businessPartnerRepository.count()
    }

    override fun deleteById(id: String) {
    }

    override fun delete(entity: BusinessPartnerDoc) {
    }

    override fun deleteAllById(ids: MutableIterable<String>) {
    }

    override fun deleteAll(entities: MutableIterable<BusinessPartnerDoc>) {
    }

    override fun deleteAll() {
    }

    override fun searchSimilar(
        entity: BusinessPartnerDoc,
        fields: Array<out String>?,
        pageable: Pageable
    ): Page<BusinessPartnerDoc> {
        return findAll(pageable)
    }
}