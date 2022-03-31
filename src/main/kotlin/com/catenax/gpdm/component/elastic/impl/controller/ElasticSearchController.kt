package com.catenax.gpdm.component.elastic.impl.controller

import com.catenax.gpdm.component.elastic.impl.service.ElasticSyncService
import com.catenax.gpdm.dto.elastic.ExportResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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

    @Operation(
        summary = "Index new business partner records on Elasticsearch",
        description = "Triggers an export of business partner records from BPDM to Elasticsearch. " +
                "Only exports records which have been updated since the last export. "
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Export of records successfully"),
            ApiResponse(responseCode = "500", description = "Export failed (no connection to Elasticsearch or database)", content = [Content()])
        ]
    )
    @PostMapping("/business-partners")
    fun export(): ExportResponse {
        return elasticSyncService.exportPartnersToElastic()
    }

    @Operation(summary = "Clear business partner index on Elasticsearch",
        description = "Deletes all business partner records in the Elasticsearch index. " +
                "Also resets the timestamp from the last export.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Index successfully cleared"),
        ApiResponse(responseCode = "500", description = "Clearing failed (no connection to Elasticsearch or database)", content = [Content()])
    ])
    @DeleteMapping("/business-partners")
    fun clear(){
        return elasticSyncService.clearElastic()
    }
}