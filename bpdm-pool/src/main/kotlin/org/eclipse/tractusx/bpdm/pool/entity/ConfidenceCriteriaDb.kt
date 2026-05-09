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

package org.eclipse.tractusx.bpdm.pool.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.time.LocalDateTime


@Embeddable
data class ConfidenceCriteriaDb(
    @Column(name = "shared_by_owner", nullable = false)
    val sharedByOwner: Boolean,
    @Column(name = "checked_by_external_data_source", nullable = false)
    val checkedByExternalDataSource: Boolean,
    @Column(name = "number_of_sharing_members", nullable = false)
    val numberOfSharingMembers: Int,
    @Column(name = "last_confidence_check_at", nullable = false)
    val lastConfidenceCheckAt: LocalDateTime,
    @Column(name = "next_confidence_check_at", nullable = false)
    val nextConfidenceCheckAt: LocalDateTime
){
    companion object{
        const val SHARED_BY_OWNER_WEIGHT = 5
        const val CHECKED_BY_EXTERNAL_DATASOURCE_WEIGHT = 3
        const val SHARING_MEMBERS_THRESHOLD = 3
        const val SHARING_MEMBERS_WEIGHT = 1
    }

    @get:Column(name = "confidence_level", nullable = false)
    val confidenceLevel: Int
        get() {
            val sharedByOwnerLevel = if(sharedByOwner) SHARED_BY_OWNER_WEIGHT else 0
            val checkedByExternalDataSourceLevel = if(checkedByExternalDataSource) CHECKED_BY_EXTERNAL_DATASOURCE_WEIGHT else 0
            val numberOfSharingMembersLevel = if(numberOfSharingMembers >= SHARING_MEMBERS_THRESHOLD) SHARING_MEMBERS_WEIGHT else 0

            return sharedByOwnerLevel + checkedByExternalDataSourceLevel + numberOfSharingMembersLevel
        }

}