package com.catenax.gpdm.controller

import com.catenax.gpdm.config.BpnConfigProperties
import com.catenax.gpdm.dto.request.IdentifiersSearchRequest
import com.catenax.gpdm.dto.response.BpnSearchResponse
import com.catenax.gpdm.service.BusinessPartnerFetchService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/catena/bpn")
class BpnController(
    val businessPartnerFetchService: BusinessPartnerFetchService,
    val bpnConfigProperties: BpnConfigProperties
) {

    @PostMapping("/search")
    fun findBpnsByIdentifiers(@RequestBody identifiersSearchRequest: IdentifiersSearchRequest): ResponseEntity<Set<BpnSearchResponse>> {
        if (identifiersSearchRequest.idValues.size > bpnConfigProperties.searchRequestLimit) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        val bpnIdentifierMappings = businessPartnerFetchService.findBpnsByIdentifiers(identifiersSearchRequest.idType, identifiersSearchRequest.idValues)
        return ResponseEntity(bpnIdentifierMappings, HttpStatus.OK)
    }
}