package org.eclipse.tractusx.bpdm.gate.controller

import org.eclipse.tractusx.bpdm.gate.dto.LegalEntityRequest
import org.eclipse.tractusx.bpdm.gate.service.LegalEntityService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/catena/legal-entities")
class LegalEntityController(
    val legalEntityService: LegalEntityService
) {

    @PutMapping
    fun upsertLegalEntities(@RequestBody legalEntityRequest: LegalEntityRequest): ResponseEntity<Any> {
        legalEntityService.upsertLegalEntities(legalEntityRequest)
        return ResponseEntity(HttpStatus.CREATED)
    }
}