package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.dto.response.BpnIdentifierMappingResponse
import org.eclipse.tractusx.bpdm.pool.entity.Identifier
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierType
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

    @Query("SELECT new org.eclipse.tractusx.bpdm.pool.dto.response.BpnIdentifierMappingResponse(i.value,i.partner.bpn) FROM Identifier i WHERE i.type = :identifierType AND i.value in :values")
    fun findBpnsByIdentifierTypeAndValues(identifierType: IdentifierType, values: Collection<String>): Set<BpnIdentifierMappingResponse>
}