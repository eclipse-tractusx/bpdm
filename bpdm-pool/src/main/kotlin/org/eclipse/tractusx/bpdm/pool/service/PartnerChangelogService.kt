/*******************************************************************************
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.pool.dto.ChangelogEntryCreateRequest
import org.eclipse.tractusx.bpdm.pool.entity.PartnerChangelogEntryDb
import org.eclipse.tractusx.bpdm.pool.repository.PartnerChangelogEntryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Records changelog entries of business partners. Changelog entries must be created manually via this service, when business partner (including
 * related child entities) are created/updated/deleted.
 *
 * The changelog entries can be used during synchronization of business partner data in order to know which business partners need to be synchronized.
 */
@Service
class PartnerChangelogService(
    private val partnerChangelogEntryRepository: PartnerChangelogEntryRepository
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun createChangelogEntries(changelogEntries: Collection<ChangelogEntryCreateRequest>): List<PartnerChangelogEntryDb> {
        logger.debug { "Create ${changelogEntries.size} new change log entries" }
        return changelogEntries.map { createChangelogEntry(it) }
    }

    fun createChangelogEntry(request: ChangelogEntryCreateRequest): PartnerChangelogEntryDb{
        logger.debug { "Create ${request.changelogType} changelog entry for ${request.bpn}" }
        return partnerChangelogEntryRepository.save(request.toEntity())
    }

    private fun ChangelogEntryCreateRequest.toEntity(): PartnerChangelogEntryDb {
        return PartnerChangelogEntryDb(this.bpn, this.businessPartnerType, this.changelogType)
    }
}
