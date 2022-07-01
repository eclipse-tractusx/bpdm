package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.entity.Address
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository

interface AddressRepository : PagingAndSortingRepository<Address, Long> {
    @Query("SELECT a FROM Address a join a.partner p where p.bpn=:bpn")
    fun findByPartnerBpn(bpn: String, pageable: Pageable): Page<Address>

    fun findByBpn(bpn: String): Address?

    @Query("SELECT DISTINCT a FROM Address a LEFT JOIN FETCH a.contexts WHERE a IN :addresses")
    fun joinContexts(addresses: Set<Address>): Set<Address>

    @Query("SELECT DISTINCT a FROM Address a LEFT JOIN FETCH a.types WHERE a IN :addresses")
    fun joinTypes(addresses: Set<Address>): Set<Address>

    @Query("SELECT DISTINCT a FROM Address a LEFT JOIN FETCH a.version WHERE a IN :addresses")
    fun joinVersions(addresses: Set<Address>): Set<Address>

    @Query("SELECT DISTINCT a FROM Address a LEFT JOIN FETCH a.administrativeAreas WHERE a IN :addresses")
    fun joinAdminAreas(addresses: Set<Address>): Set<Address>

    @Query("SELECT DISTINCT a FROM Address a LEFT JOIN FETCH a.postCodes WHERE a IN :addresses")
    fun joinPostCodes(addresses: Set<Address>): Set<Address>

    @Query("SELECT DISTINCT a FROM Address a LEFT JOIN FETCH a.thoroughfares WHERE a IN :addresses")
    fun joinThoroughfares(addresses: Set<Address>): Set<Address>

    @Query("SELECT DISTINCT a FROM Address a LEFT JOIN FETCH a.premises WHERE a IN :addresses")
    fun joinPremises(addresses: Set<Address>): Set<Address>

    @Query("SELECT DISTINCT a FROM Address a LEFT JOIN FETCH a.postalDeliveryPoints WHERE a IN :addresses")
    fun joinPostalDeliveryPoints(addresses: Set<Address>): Set<Address>

    @Query("SELECT DISTINCT a FROM Address a LEFT JOIN FETCH a.localities WHERE a IN :addresses")
    fun joinLocalities(addresses: Set<Address>): Set<Address>

}