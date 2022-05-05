package com.catenax.gpdm.repository

import com.catenax.gpdm.dto.response.BpnSearchResponse
import com.catenax.gpdm.entity.Identifier
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface IdentifierRepository : CrudRepository<Identifier, Long> {
    fun findByUuidIn(uuid: Collection<UUID>): Set<Identifier>

    @Query("SELECT DISTINCT i FROM Identifier i LEFT JOIN FETCH i.type WHERE i IN :identifiers")
    fun joinType(identifiers: Set<Identifier>): Set<Identifier>

    @Query("SELECT DISTINCT i FROM Identifier i LEFT JOIN FETCH i.status WHERE i IN :identifiers")
    fun joinStatus(identifiers: Set<Identifier>): Set<Identifier>

    @Query("SELECT DISTINCT i FROM Identifier i LEFT JOIN FETCH i.issuingBody WHERE i IN :identifiers")
    fun joinIssuingBody(identifiers: Set<Identifier>): Set<Identifier>

    @Query("SELECT new com.catenax.gpdm.dto.response.BpnSearchResponse(i.value,i.partner.bpn) FROM Identifier i WHERE i.type.technicalKey = ?1 AND i.value in ?2")
    fun findBpnsByIdentifierTypeAndValues(type: String, values: Collection<String>): Set<BpnSearchResponse>
}