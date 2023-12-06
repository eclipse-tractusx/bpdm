/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

import com.neovisionaries.i18n.CountryCode
import org.eclipse.tractusx.bpdm.pool.api.model.IdentifierBusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierType
import org.eclipse.tractusx.bpdm.pool.entity.IdentifierTypeDetail
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository

interface IdentifierTypeRepository :
    PagingAndSortingRepository<IdentifierType, Long>, CrudRepository<IdentifierType, Long>, JpaSpecificationExecutor<IdentifierType> {

    object Specs {
        fun byBusinessPartnerType(businessPartnerType: IdentifierBusinessPartnerType) =
            Specification<IdentifierType> { root, _, builder ->
                builder.equal(root.get<IdentifierBusinessPartnerType>(IdentifierType::businessPartnerType.name), businessPartnerType)
            }

        fun byCountry(countryCode: CountryCode?) =
            countryCode?.let {
                Specification<IdentifierType> { root, query, builder ->
                    val subquery = query.subquery(IdentifierTypeDetail::class.java)
                    val subRoot = subquery.from(subquery.resultType)

                    // Check if an IdentifierTypeDetail exists for the IdentifierType where the countryCode is null or equal to given countryCode
                    builder.exists(
                        subquery.where(
                            builder.equal(
                                subRoot.get<IdentifierType>(IdentifierTypeDetail::identifierType.name),
                                root
                            ),
                            builder.or(
                                subRoot.get<CountryCode>(IdentifierTypeDetail::countryCode.name).isNull,
                                builder.equal(
                                    subRoot.get<CountryCode>(IdentifierTypeDetail::countryCode.name),
                                    countryCode
                                )
                            )
                        )
                    )
                }
            }
    }

    fun findByBusinessPartnerTypeAndTechnicalKey(businessPartnerType: IdentifierBusinessPartnerType, key: String): IdentifierType?
    fun findByBusinessPartnerTypeAndTechnicalKeyIn(businessPartnerType: IdentifierBusinessPartnerType, technicalKeys: Set<String>): Set<IdentifierType>
}