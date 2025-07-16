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
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDb
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.NameDb
import org.eclipse.tractusx.bpdm.pool.entity.PhysicalPostalAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.StreetDb
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query

interface LegalEntityRepository : JpaRepository<LegalEntityDb, Long>, JpaSpecificationExecutor<LegalEntityDb> {

    companion object {
        fun byBpns(bpns: Collection<String>?) =
            Specification<LegalEntityDb> { root, _, _ ->
                bpns?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }?.let {
                    root.get<String>(LegalEntityDb::bpn.name).`in`(bpns)
                }
            }

        fun byLegalName(legalName: String?) =
            Specification<LegalEntityDb> { root, _, builder ->
                legalName?.takeIf { it.isNotBlank() }?.let {
                    builder.like(builder.lower(root.get<NameDb>(LegalEntityDb::legalName.name).get(NameDb::value.name)), "%${legalName.lowercase()}%")
                }
            }

        fun byIsMember(isCatenaXMemberData: Boolean?) =
            Specification<LegalEntityDb> { root, _, builder ->
                isCatenaXMemberData?.let {
                    builder.equal(root.get<Boolean>(LegalEntityDb::isCatenaXMemberData.name), isCatenaXMemberData)
                }
            }

        /*
        business-partner/search
        */
        fun buildLegalEntitySpecification(request: LegalEntityPropertiesSearchRequest): Specification<LegalEntityDb> {

            return Specification.where(byBpnL(request.id))
                .and(byLegalNameSupportGermanUmlauts(request.legalName))
                .and(byStreet(request.street))
                .and(byZipCode(request.postcode))
                .and(byCity(request.city))
                .and(byCountry(request.country))
        }

        fun byBpnL(bpnl: String?) =
            Specification<LegalEntityDb> { root, _, builder ->
                bpnl?.takeIf { it.isNotEmpty() }?.let {
                    builder.equal(root.get<String>(LegalEntityDb::bpn.name), bpnl)
                }
            }

        fun byLegalNameSupportGermanUmlauts(legalName: String?) =
            Specification<LegalEntityDb> { root, _, builder ->
                legalName?.takeIf { it.isNotBlank() }?.let {
                    val normalized = normalizeGermanUmlauts(legalName)
                    val expression = builder.lower(root.get<NameDb>(LegalEntityDb::legalName.name).get(NameDb::value.name))
                    val predicates = normalized.map { variant ->
                        builder.like(expression, "%$variant%")
                    }

                    builder.or(*predicates.toTypedArray())
                }
            }

        fun byStreet(street: String?) =
            Specification<LegalEntityDb> { root, _, builder ->
                street?.takeIf { it.isNotBlank() }?.let {
                    val joinAddress = root.join<LegalEntityDb, LogisticAddressDb>("addresses")
                    builder.like (
                        builder.lower(
                            joinAddress
                                .get<PhysicalPostalAddressDb>("physicalPostalAddress")
                                .get<StreetDb>("street")
                                .get<String>("name")
                        ),
                        "%${it.lowercase()}%"
                    )
                }
            }

        fun byZipCode(zipCode: String?) =
            Specification<LegalEntityDb> { root, _, builder ->
                zipCode?.takeIf { it.isNotBlank() }?.let {
                    val joinAddress = root.join<LegalEntityDb, LogisticAddressDb>("addresses")
                    builder.like (
                        builder.lower(
                            joinAddress.get<PhysicalPostalAddressDb>("physicalPostalAddress").get("postCode")
                        ),
                        "%${it.lowercase()}%"
                    )
                }
            }

        fun byCity(city: String?) =
            Specification<LegalEntityDb> { root, _, builder ->
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

        fun byCountry(country: String?) =
            Specification<LegalEntityDb> { root, _, builder ->
                country?.takeIf { it.isNotBlank() }?.let {
                    val joinAddress = root.join<LegalEntityDb, LogisticAddressDb>("addresses")
                    builder.like (
                        builder.lower(
                            joinAddress.get<PhysicalPostalAddressDb>("physicalPostalAddress").get("country")
                        ),
                        "%${it.lowercase()}%"
                    )
                }
            }

        fun normalizeGermanUmlauts(input: String): List<String> {
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

    fun findByBpnIgnoreCase(bpn: String): LegalEntityDb?

    fun existsByBpn(bpn: String): Boolean

    fun findDistinctByBpnIn(bpns: Collection<String>): Set<LegalEntityDb>

    @Query("SELECT p FROM LegalEntityDb p WHERE LOWER(p.legalName.value) LIKE :value ORDER BY LENGTH(p.legalName.value)")
    fun findByLegalNameValue(value: String, pageable: Pageable): Page<LegalEntityDb>

    @Query("SELECT DISTINCT i.legalEntity FROM LegalEntityIdentifierDb i WHERE i.type = :type AND upper(i.value) = upper(:idValue)")
    fun findByIdentifierTypeAndValueIgnoreCase(type: IdentifierTypeDb, idValue: String): LegalEntityDb?

    @Query("SELECT DISTINCT p FROM LegalEntityDb p LEFT JOIN FETCH p.legalForm WHERE p IN :partners")
    fun joinLegalForm(partners: Set<LegalEntityDb>): Set<LegalEntityDb>

    @Query("SELECT DISTINCT p FROM LegalEntityDb p LEFT JOIN FETCH p.identifiers WHERE p IN :partners")
    fun joinIdentifiers(partners: Set<LegalEntityDb>): Set<LegalEntityDb>

    @Query("SELECT DISTINCT p FROM LegalEntityDb p LEFT JOIN FETCH p.states WHERE p IN :partners")
    fun joinStates(partners: Set<LegalEntityDb>): Set<LegalEntityDb>

    @Query("SELECT DISTINCT p FROM LegalEntityDb p LEFT JOIN FETCH p.classifications WHERE p IN :partners")
    fun joinClassifications(partners: Set<LegalEntityDb>): Set<LegalEntityDb>

    @Query("SELECT DISTINCT p FROM LegalEntityDb p LEFT JOIN FETCH p.startNodeRelations LEFT JOIN FETCH p.endNodeRelations WHERE p IN :partners")
    fun joinRelations(partners: Set<LegalEntityDb>): Set<LegalEntityDb>

    @Query("SELECT DISTINCT p FROM LegalEntityDb p LEFT JOIN FETCH p.legalAddress WHERE p IN :partners")
    fun joinLegalAddresses(partners: Set<LegalEntityDb>): Set<LegalEntityDb>
}