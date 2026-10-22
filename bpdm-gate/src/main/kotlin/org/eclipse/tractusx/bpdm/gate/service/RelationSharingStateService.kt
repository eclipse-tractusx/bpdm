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

import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateErrorCode
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType
import org.eclipse.tractusx.bpdm.gate.entity.RelationDb
import org.eclipse.tractusx.bpdm.gate.entity.RelationSharingStateDb
import org.eclipse.tractusx.bpdm.gate.repository.RelationRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RelationSharingStateService(
    private val relationRepository: RelationRepository
) {

    fun setInitial(relation: RelationDb, relationType: RelationType){
        relation.sharingState = RelationSharingStateDb(
            sharingStateType = RelationSharingStateType.Ready,
            sharingErrorCode = null,
            sharingErrorMessage = null,
            recordId = relation.sharingState?.recordId,
            taskId = null,
            updatedAt = Instant.now(),
            isStaged = false
        ).takeIf { canBeShared(relationType) }

        relationRepository.save(relation)
    }

    fun setPending(relation: RelationDb, taskId: String, recordId: String){
        relation.sharingState = RelationSharingStateDb(
            sharingStateType = RelationSharingStateType.Pending,
            sharingErrorCode = null,
            sharingErrorMessage = null,
            recordId = recordId,
            taskId = taskId,
            updatedAt = Instant.now(),
            isStaged = false
        )

        relationRepository.save(relation)
    }

    fun setSuccess(relation: RelationDb){
        relation.sharingState = RelationSharingStateDb(
            sharingStateType = RelationSharingStateType.Success,
            sharingErrorCode = null,
            sharingErrorMessage = null,
            recordId = relation.sharingState?.recordId,
            taskId = relation.sharingState?.taskId,
            updatedAt = Instant.now(),
            isStaged = false
        )
    }

    fun setError(relation: RelationDb, errorCode: RelationSharingStateErrorCode, errorMessage: String){
        relation.sharingState = RelationSharingStateDb(
            sharingStateType = RelationSharingStateType.Error,
            sharingErrorCode = errorCode,
            sharingErrorMessage = errorMessage,
            recordId = relation.sharingState?.recordId,
            taskId = relation.sharingState?.taskId,
            updatedAt = Instant.now(),
            isStaged = false
        )
    }

    private fun canBeShared(relationType: RelationType): Boolean{
        return when(relationType){
            RelationType.IsManagedBy -> true
            RelationType.IsAlternativeHeadquarterFor -> true
            RelationType.IsOwnedBy -> true
            RelationType.IsReplacedBy -> true
        }
    }
}