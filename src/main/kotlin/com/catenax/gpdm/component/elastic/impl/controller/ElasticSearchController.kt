package com.catenax.gpdm.component.elastic.impl.controller


import com.catenax.gpdm.component.elastic.impl.service.ElasticSyncStarterService
import com.catenax.gpdm.dto.response.SyncResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/elastic")
class ElasticSearchController(
    val elasticSyncService: ElasticSyncStarterService
) {

    @Operation(
        summary = "Index new business partner records on Elasticsearch",
        description = "Triggers an asynchronous export of business partner records from BPDM to Elasticsearch. " +
                "Only exports records which have been updated since the last export. "
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Export of records successfully"),
            ApiResponse(responseCode = "500", description = "Export failed (no connection to Elasticsearch or database)", content = [Content()])
        ]
    )
    @PostMapping("/business-partner")
    fun export(): SyncResponse {
        return elasticSyncService.exportAsync()
    }

    @Operation(
        summary = "Fetch information about the latest Elasticsearch export",
        description = "Fetch information about the latest export (either ongoing or already finished)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Export information found"),
            ApiResponse(responseCode = "500", description = "Fetching failed (no connection to database)", content = [Content()])
        ]
    )
    @GetMapping("/business-partner")
    fun getBusinessPartners(): SyncResponse {
        return elasticSyncService.getExportStatus()
    }


    @Operation(
        summary = "Clear business partner index on Elasticsearch",
        description = "Deletes all business partner records in the Elasticsearch index. " +
                "Also resets the timestamp from the last export."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Index successfully cleared"),
            ApiResponse(responseCode = "500", description = "Clearing failed (no connection to Elasticsearch or database)", content = [Content()])
        ]
    )
    @DeleteMapping("/business-partner")
    fun clear(){
        elasticSyncService.clearElastic()
    }
}