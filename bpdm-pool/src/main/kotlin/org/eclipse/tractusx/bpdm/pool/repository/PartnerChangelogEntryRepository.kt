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

import org.eclipse.tractusx.bpdm.pool.entity.ChangelogSubject
import org.eclipse.tractusx.bpdm.pool.entity.PartnerChangelogEntry
import org.eclipse.tractusx.bpdm.pool.entity.PartnerChangelogEntry_
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.time.Instant

interface PartnerChangelogEntryRepository : JpaRepository<PartnerChangelogEntry, Long>, JpaSpecificationExecutor<PartnerChangelogEntry> {
    companion object Specs {
        /**
         * Restrict to entries with any one of the given BPNs; ignore if null
         */
        fun byBpnsIn(bpns: Array<String>?) =
            Specification<PartnerChangelogEntry> { root, _, _ ->
                bpns?.let {
                    root.get(PartnerChangelogEntry_.bpn).`in`(bpns.map { bpn -> bpn.uppercase() })
                }
            }

        /**
         * Restrict to entries updated after the given instant; ignore if null
         */
        fun byUpdatedGreaterThan(modifiedAfter: Instant?) =
            Specification<PartnerChangelogEntry> { root, _, builder ->
                modifiedAfter?.let {
                    builder.greaterThanOrEqualTo(root.get(PartnerChangelogEntry_.updatedAt), modifiedAfter)
                }
            }
    }

    fun findAllByIdGreaterThan(id: Long, pageable: Pageable): Page<PartnerChangelogEntry>

    fun findByCreatedAtAfterAndChangelogSubjectIn(
        createdAt: Instant,
        changelogSubject: Collection<ChangelogSubject>,
        pageable: Pageable
    ): Page<PartnerChangelogEntry>
}