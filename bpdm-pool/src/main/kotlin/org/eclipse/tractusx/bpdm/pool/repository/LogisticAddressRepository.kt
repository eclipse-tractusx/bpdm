/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.pool.repository

import org.eclipse.tractusx.bpdm.pool.api.model.request.LegalEntityPropertiesSearchRequest
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.NameDb
import org.eclipse.tractusx.bpdm.pool.entity.PhysicalPostalAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.eclipse.tractusx.bpdm.pool.entity.StreetDb
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query

interface LogisticAddressRepository : JpaRepository<LogisticAddressDb, Long>, JpaSpecificationExecutor<LogisticAddressDb> {

    companion object {
        fun byBpns(bpns: Collection<String>?) =
            Specification<LogisticAddressDb> { root, _, _ ->
                bpns?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }?.let {
                    root.get<String>(LogisticAddressDb::bpn.name).`in`(bpns)
                }
            }

        fun bySiteBpns(bpns: Collection<String>?) =
            Specification<LogisticAddressDb> { root, _, _ ->
                bpns?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }?.let {
                    root.get<LegalEntityDb>(LogisticAddressDb::site.name).get<String>(SiteDb::bpn.name).`in`(bpns)
                }
            }

        fun byLegalEntityBpns(bpns: Collection<String>?) =
            Specification<LogisticAddressDb> { root, _, _ ->
                bpns?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }?.let {
                    root.get<LegalEntityDb>(LogisticAddressDb::legalEntity.name).get<String>(LegalEntityDb::bpn.name).`in`(bpns)
                }
            }

        fun byName(name: String?) =
            Specification<LogisticAddressDb> { root, _, builder ->
                name?.takeIf { it.isNotBlank() }?.let {
                    builder.like(builder.lower(root.get(LogisticAddressDb::name.name)), "%${name.lowercase()}%")
                }
            }

        fun byIsMember(isCatenaXMemberData: Boolean?) =
            Specification<LogisticAddressDb> { root, _, builder ->
                isCatenaXMemberData?.let {
                    builder.equal(
                        root.get<LegalEntityDb>(LogisticAddressDb::legalEntity.name)
                            .get<Boolean>(LegalEntityDb::isCatenaXMemberData.name),
                        isCatenaXMemberData
                    )
                }
            }
        /*
        business-partner/search
        */
        fun buildAddressSpecification(request: LegalEntityPropertiesSearchRequest): Specification<LogisticAddressDb> {

            return Specification.where(byBpnA(request.id))
                .and(byStreet(request.street))
                .and(byZipCode(request.postcode))
                .and(byCity(request.city))
                .and(byCountry(request.country))
        }

        private fun byBpnA(bpna: String?) =
            Specification<LogisticAddressDb> { root, _, builder ->
                bpna?.takeIf { it.isNotEmpty() }?.let {
                    builder.equal(root.get<String>(LogisticAddressDb::bpn.name), bpna)
                }
            }

        private fun byStreet(street: String?) =
            Specification<LogisticAddressDb> { root, _, builder ->
                street?.takeIf { it.isNotBlank() }?.let {
                    builder.equal(root.get<LogisticAddressDb>("physicalPostalAddress")
                        .get<StreetDb>("street")
                        .get<String>("name"),
                        "%${it.lowercase()}%")
                }
            }

        private fun byZipCode(zipCode: String?) =
            Specification<LogisticAddressDb> { root, _, builder ->
                zipCode?.takeIf { it.isNotBlank() }?.let {
                    builder.equal(root.get<PhysicalPostalAddressDb>("physicalPostalAddress").get<String>("postCode"), "%${it.lowercase()}%")
                }
            }

        private fun byCity(city: String?) =
            Specification<LogisticAddressDb> { root, _, builder ->
                city?.takeIf { it.isNotBlank() }?.let {
                    val normalizedVariants = normalizeGermanUmlauts(city)
                    val joinAddress = root.join<LegalEntityDb, LogisticAddressDb>("addresses")
                    val expression = builder.lower(joinAddress.get<PhysicalPostalAddressDb>("physicalPostalAddress").get("city"))
                    val predicates = normalizedVariants.map { variant ->
                        builder.like(expression, "%$variant%")
                    }

                    builder.or(*predicates.toTypedArray())
                }
            }

        private fun byCountry(country: String?) =
            Specification<LogisticAddressDb> { root, _, builder ->
                country?.takeIf { it.isNotBlank() }?.let {
                    builder.equal(root.get<LogisticAddressDb>("physicalPostalAddress").get<String>("country"),"%${it.lowercase()}%")
                }
            }

        private fun normalizeGermanUmlauts(input: String): List<String> {
            val base = input.lowercase();
            return listOf(
                base,
                base.replace("ae", "ä"),
                base.replace("oe", "ö"),
                base.replace("ue", "ü"),
                base.replace("ss", "ß")
            ).distinct()
        }
    }

    fun findByBpn(bpn: String): LogisticAddressDb?

    fun findDistinctByBpnIn(bpns: Collection<String>): Set<LogisticAddressDb>

    fun findByLegalEntityAndSiteIsNull(legalEntityDb: LegalEntityDb, pageable: Pageable): Page<LogisticAddressDb>

    @Query("SELECT a FROM LogisticAddressDb a join a.legalEntity p where p.bpn=:bpn")
    fun findByLegalEntityBpn(bpn: String, pageable: Pageable): Page<LogisticAddressDb>

    fun findByLegalEntityInOrSiteInOrBpnIn(
        legalEntities: Collection<LegalEntityDb>,
        sites: Collection<SiteDb>,
        bpns: Collection<String>,
        pageable: Pageable
    ): Page<LogisticAddressDb>

    @Query("SELECT a FROM LogisticAddressDb a WHERE LOWER(a.name) LIKE :addressName ORDER BY LENGTH(a.name)")
    fun findByName(addressName: String, pageable: Pageable): Page<LogisticAddressDb>

    @Query("SELECT DISTINCT a FROM LogisticAddressDb a LEFT JOIN FETCH a.legalEntity LEFT JOIN FETCH a.legalEntity.legalAddress WHERE a IN :addresses")
    fun joinLegalEntities(addresses: Set<LogisticAddressDb>): Set<LogisticAddressDb>

    @Query("SELECT DISTINCT a FROM LogisticAddressDb a LEFT JOIN FETCH a.site LEFT JOIN FETCH a.site.mainAddress WHERE a IN :addresses")
    fun joinSites(addresses: Set<LogisticAddressDb>): Set<LogisticAddressDb>

    @Query("SELECT DISTINCT a FROM LogisticAddressDb a LEFT JOIN FETCH a.physicalPostalAddress.administrativeAreaLevel1 LEFT JOIN FETCH a.alternativePostalAddress.administrativeAreaLevel1 WHERE a IN :addresses")
    fun joinRegions(addresses: Set<LogisticAddressDb>): Set<LogisticAddressDb>

    @Query("SELECT DISTINCT p FROM LogisticAddressDb p LEFT JOIN FETCH p.identifiers WHERE p IN :partners")
    fun joinIdentifiers(partners: Set<LogisticAddressDb>): Set<LogisticAddressDb>

    @Query("SELECT DISTINCT p FROM LogisticAddressDb p LEFT JOIN FETCH p.states WHERE p IN :partners")
    fun joinStates(partners: Set<LogisticAddressDb>): Set<LogisticAddressDb>
}