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

package org.eclipse.tractusx.bpdm.gate.service

import mu.KotlinLogging
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntryDb
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.springframework.stereotype.Service

/**
 * The single authority for writing changelog entries of business partners and relations.
 */
@Service
class ChangelogCreateService(
    private val changelogRepository: ChangelogRepository
) {
    private val logger = KotlinLogging.logger { }

    /**
     * Writes the given changelog entry.
     */
    fun record(entry: ChangelogEntryDb) {
        changelogRepository.save(entry)
        logger.debug {
            "Created ${entry.changelogType} changelog entry for ${entry.goldenRecordType} ${entry.stage} '${entry.externalId}'"
        }
    }
}
