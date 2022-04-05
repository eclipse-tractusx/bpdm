package com.catenax.gpdm.component.cdq.controller

import com.catenax.gpdm.component.cdq.dto.ExportResponse
import com.catenax.gpdm.component.cdq.dto.ImportResponse
import com.catenax.gpdm.component.cdq.service.PartnerExportService
import com.catenax.gpdm.component.cdq.service.PartnerImportService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/cdq")
class CdqController(
    val partnerImportService: PartnerImportService,
    val partnerExportService: PartnerExportService
) {
    @Operation(
        summary = "Import new business partner records from CDQ",
        description = "Triggers an import of new business partner records from CDQ. " +
                "A CDQ record counts as new when it does not have a BPN and the BPDM service does not already have a record with the same CDQ ID. " +
                "This import only regards records with a modifiedAfter timestamp since the last import."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Import performed successfully"),
            ApiResponse(responseCode = "500", description = "Import failed (no connection to CDQ or database)", content = [Content()])
        ]
    )
    @PostMapping("/business-partners/import")
    fun importBusinessPartners(): ImportResponse {
        return partnerImportService.import()
    }

    @Operation(
        summary = "Export of BPNs to CDQ records",
        description = "Triggers an export of BPNs from BPDM to CDQ. " +
                "Regards business partner records in the BPDM system without a synchronized CDQ identifier status. " +
                "Only exports BPNs and keeps other properties of the CDQ record untouched."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Export of BPNs performed successfully"),
            ApiResponse(responseCode = "500", description = "Export failed (no connection to CDQ or database)", content = [Content()])
        ]
    )
    @PostMapping("/business-partners/export")
    fun exportBusinessPartners(): ExportResponse {
        return partnerExportService.export()
    }
}