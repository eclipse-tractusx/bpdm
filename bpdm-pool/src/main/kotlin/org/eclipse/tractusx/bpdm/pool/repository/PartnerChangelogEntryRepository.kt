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

import org.eclipse.tractusx.bpdm.common.dto.BusinessPartnerType
import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.entity.LogisticAddressDb
import org.eclipse.tractusx.bpdm.pool.entity.PartnerChangelogEntryDb
import org.eclipse.tractusx.bpdm.pool.entity.SiteDb
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.time.Instant

interface PartnerChangelogEntryRepository : JpaRepository<PartnerChangelogEntryDb, Long>, JpaSpecificationExecutor<PartnerChangelogEntryDb> {
    object Specs {
        /**
         * Restrict to entries with any one of the given BPNs; ignore if empty
         */
        fun byBpnsIn(bpns: Set<String>?) =
            Specification<PartnerChangelogEntryDb> { root, _, _ ->
                bpns?.let {
                    if (bpns.isNotEmpty())
                        root.get<String>(PartnerChangelogEntryDb::bpn.name).`in`(bpns.map { bpn -> bpn.uppercase() })
                    else
                        null
                }
            }

        /**
         * Restrict to entries updated after the given instant; ignore if null
         */
        fun byUpdatedGreaterThan(modifiedAfter: Instant?) =
            Specification<PartnerChangelogEntryDb> { root, _, builder ->
                modifiedAfter?.let {
                    builder.greaterThan(root.get(PartnerChangelogEntryDb::updatedAt.name), modifiedAfter)
                }
            }

        /**
         * Restrict to entries with any one of the given LSA types; ignore if empty
         */
        fun byBusinessPartnerTypesIn(businessPartnerTypes: Set<BusinessPartnerType>?) =
            Specification<PartnerChangelogEntryDb> { root, _, _ ->
                businessPartnerTypes?.let {
                    if (businessPartnerTypes.isNotEmpty())
                        root.get<String>(PartnerChangelogEntryDb::businessPartnerType.name).`in`(businessPartnerTypes)
                    else
                        null
                }
            }

        fun byIsMember(isCatenaXMemberData: Boolean?) =
            Specification<PartnerChangelogEntryDb> { root, query, builder ->
                isCatenaXMemberData?.let {

                    val legalEntitySubquery = query.subquery(PartnerChangelogEntryDb::class.java)
                    val changelogSubRoot = legalEntitySubquery.from(PartnerChangelogEntryDb::class.java)
                    val legalEntitySubRoot = legalEntitySubquery.from(LegalEntityDb::class.java)
                    legalEntitySubquery.select(changelogSubRoot)
                    legalEntitySubquery.where( builder.and(
                        builder.equal(legalEntitySubRoot.get<Boolean>(LegalEntityDb::isCatenaXMemberData.name), isCatenaXMemberData),
                        builder.equal(changelogSubRoot.get<String>("bpn"), legalEntitySubRoot.get<String>("bpn")),
                    ))

                    val siteSubquery = query.subquery(PartnerChangelogEntryDb::class.java)
                    val chSiteSubRoot = siteSubquery.from(PartnerChangelogEntryDb::class.java)
                    val lSiteSubRoot = siteSubquery.from(LegalEntityDb::class.java)
                    val sSiteSubRoot = siteSubquery.from(SiteDb::class.java)
                    siteSubquery.select(chSiteSubRoot)
                    siteSubquery.where( builder.and(
                        builder.equal(lSiteSubRoot.get<Boolean>(LegalEntityDb::isCatenaXMemberData.name), isCatenaXMemberData),
                        builder.equal(chSiteSubRoot.get<String>("bpn"), sSiteSubRoot.get<String>("bpn")),
                        builder.equal(sSiteSubRoot.get<LegalEntityDb>(SiteDb::legalEntity.name), lSiteSubRoot),
                    ))

                    val addressSubquery = query.subquery(PartnerChangelogEntryDb::class.java)
                    val chAddressSubRoot = addressSubquery.from(PartnerChangelogEntryDb::class.java)
                    val lAddressSubRoot = addressSubquery.from(LegalEntityDb::class.java)
                    val sAddressSubRoot = addressSubquery.from(LogisticAddressDb::class.java)
                    addressSubquery.select(chAddressSubRoot)
                    addressSubquery.where( builder.and(
                        builder.equal(lAddressSubRoot.get<Boolean>(LegalEntityDb::isCatenaXMemberData.name), isCatenaXMemberData),
                        builder.equal(chAddressSubRoot.get<String>("bpn"), sAddressSubRoot.get<String>("bpn")),
                        builder.equal(sAddressSubRoot.get<LegalEntityDb>(LogisticAddressDb::legalEntity.name), lAddressSubRoot),
                    ))

                    builder.or(
                        root.`in`(legalEntitySubquery),
                        root.`in`(siteSubquery),
                        root.`in`(addressSubquery)
                    )
                }
            }
    }

    fun findByCreatedAtAfterAndBusinessPartnerTypeIn(
        createdAt: Instant,
        businessPartnerType: Collection<BusinessPartnerType>,
        pageable: Pageable
    ): Page<PartnerChangelogEntryDb>
}