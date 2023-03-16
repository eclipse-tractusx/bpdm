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

package org.eclipse.tractusx.bpdm.gate.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.gate.dto.response.LsaType
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntity
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ChangelogService (private val changelogRepository: ChangelogRepository) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun createChangelog(externalId: String) {
        val changelogEntity = ChangelogEntity(externalId,LsaType.Address)
        changelogRepository.save(changelogEntity)
    }


    fun getChangeLog(externalIds: Collection<String>, lsaType: LsaType?, fromTime: Instant?): List<ChangelogEntity> {
        return changelogRepository.findAllByExternalIdInAndBusinessPartnerTypeAndCreatedAtGreaterThanEqual(
            externalIds = externalIds,
            businessPartnerType = lsaType.toString(),
            createdAt = fromTime
        )
    }


}