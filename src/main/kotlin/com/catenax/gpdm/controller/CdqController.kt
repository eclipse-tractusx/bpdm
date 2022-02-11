package com.catenax.gpdm.controller

import com.catenax.gpdm.adapter.cdq.PartnerImportService
import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("cdq")
class CdqController(
    val partnerImportService: PartnerImportService
) {

    @GetMapping("/business-partners")
    fun getBusinessPartners(): Collection<BusinessPartnerResponse> {
        return partnerImportService.import()
    }
}