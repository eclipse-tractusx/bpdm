package com.catenax.gpdm.controller

import com.catenax.gpdm.dto.request.IdentifiersSearchRequest
import com.catenax.gpdm.dto.response.BpnSearchResponse
import com.catenax.gpdm.service.BusinessPartnerFetchService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/catena/bpn")
class BpnController(
    val businessPartnerFetchService: BusinessPartnerFetchService
) {

    @PostMapping("/search")
    fun findBpnsByIdentifiers(@RequestBody identifiersSearchRequest: IdentifiersSearchRequest): Set<BpnSearchResponse> {
        return businessPartnerFetchService.findBpnsByIdentifiers(identifiersSearchRequest.idType, identifiersSearchRequest.idValues)
    }
}