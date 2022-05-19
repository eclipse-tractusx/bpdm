package com.catenax.gpdm.repository.entity

import com.catenax.gpdm.entity.Address
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository

interface AddressRepository : PagingAndSortingRepository<Address, Long> {

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