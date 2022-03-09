package com.catenax.gpdm.controller

import com.catenax.gpdm.dto.elastic.BusinessPartnerDoc
import com.catenax.gpdm.service.ElasticSyncService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/elastic")
@ConditionalOnProperty(
    value = ["bpdm.elastic.enabled"],
    havingValue = "true",
    matchIfMissing = false)
class ElasticSearchController(
    val elasticSyncService: ElasticSyncService
) {

    @PostMapping("/business-partners")
    fun export(): Collection<BusinessPartnerDoc> {
        return elasticSyncService.exportPartnersToElastic()
    }

    @DeleteMapping("/business-partners")
    fun clear(){
        return elasticSyncService.clearElastic()
    }
}