package com.catenax.gpdm.component.cdq.controller

import com.catenax.gpdm.component.cdq.service.PartnerExportPageService
import com.catenax.gpdm.component.cdq.service.PartnerImportService
import com.catenax.gpdm.component.cdq.dto.BusinessPartnerCdq
import com.catenax.gpdm.dto.response.BusinessPartnerResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/cdq")
class CdqController(
    val partnerImportService: PartnerImportService,
    val partnerExportPageService: PartnerExportPageService
) {

    @PostMapping("/business-partners/import")
    fun importBusinessPartners(): Collection<BusinessPartnerResponse> {
        return partnerImportService.import()
    }

    @PostMapping("/business-partners/export")
    fun getBusinessPartners(): Collection<BusinessPartnerCdq> {
        return partnerExportPageService.export()
    }
}