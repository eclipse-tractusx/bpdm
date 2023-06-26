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
import org.eclipse.tractusx.bpdm.common.dto.response.PageResponse
import org.eclipse.tractusx.bpdm.common.exception.BpdmNotFoundException
import org.eclipse.tractusx.bpdm.common.model.OutputInputEnum
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateInputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateOutputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.LegalEntityGateOutputResponse
import org.eclipse.tractusx.bpdm.gate.api.model.LsaType
import org.eclipse.tractusx.bpdm.gate.entity.ChangelogEntry
import org.eclipse.tractusx.bpdm.gate.entity.LegalEntity
import org.eclipse.tractusx.bpdm.gate.repository.ChangelogRepository
import org.eclipse.tractusx.bpdm.gate.repository.LegalEntityRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class LegalEntityService(
    private val changelogRepository: ChangelogRepository,
    private val legalEntityPersistenceService: LegalEntityPersistenceService,
    private val legalEntityRepository: LegalEntityRepository
) {

    private val logger = KotlinLogging.logger { }

    /**
     * Upsert legal entities input to the database
     **/
    fun upsertLegalEntities(legalEntities: Collection<LegalEntityGateInputRequest>) {

        legalEntities.forEach { legalEntity ->
            changelogRepository.save(ChangelogEntry(legalEntity.externalId, LsaType.LegalEntity))
        }
        legalEntityPersistenceService.persistLegalEntitiesBP(legalEntities, OutputInputEnum.Input)
    }

    /**
     * Upsert legal entities output to the database
     **/
    fun upsertLegalEntitiesOutput(legalEntities: Collection<LegalEntityGateOutputRequest>) {

        legalEntityPersistenceService.persistLegalEntitiesOutputBP(legalEntities, OutputInputEnum.Output)
    }

    fun getLegalEntityByExternalId(externalId: String): LegalEntityGateInputResponse {

        val legalEntity =
            legalEntityRepository.findByExternalIdAndDataType(externalId, OutputInputEnum.Input) ?: throw BpdmNotFoundException("LegalEntity", externalId)
        return toValidSingleLegalEntity(legalEntity)
    }

    fun getLegalEntities(page: Int, size: Int, externalIds: Collection<String>? = null): PageResponse<LegalEntityGateInputResponse> {

        val legalEntitiesPage = if (externalIds != null) {
            legalEntityRepository.findByExternalIdInAndDataType(externalIds, OutputInputEnum.Input, PageRequest.of(page, size))
        } else {
            legalEntityRepository.findByDataType(OutputInputEnum.Input, PageRequest.of(page, size))
        }

        return PageResponse(
            page = page,
            totalElements = legalEntitiesPage.totalElements,
            totalPages = legalEntitiesPage.totalPages,
            contentSize = legalEntitiesPage.content.size,
            content = toValidLegalEntities(legalEntitiesPage)
        )
    }

    /**
     * Get output legal entities by first fetching legal entities from the database
     */
    fun getLegalEntitiesOutput(externalIds: Collection<String>?, page: Int, size: Int): PageResponse<LegalEntityGateOutputResponse> {

        val legalEntityPage = if (externalIds != null && externalIds.isNotEmpty()) {
            legalEntityRepository.findByExternalIdInAndDataType(externalIds, OutputInputEnum.Output, PageRequest.of(page, size))
        } else {
            legalEntityRepository.findByDataType(OutputInputEnum.Output, PageRequest.of(page, size))
        }

        return PageResponse(
            page = page,
            totalElements = legalEntityPage.totalElements,
            totalPages = legalEntityPage.totalPages,
            contentSize = legalEntityPage.content.size,
            content = toValidOutputLegalEntities(legalEntityPage),
        )

    }

    private fun toValidOutputLegalEntities(legalEntityPage: Page<LegalEntity>): List<LegalEntityGateOutputResponse> {
        return legalEntityPage.content.map { legalEntity ->
            legalEntity.toLegalEntityGateOutputResponse(legalEntity)
        }
    }

    private fun toValidLegalEntities(legalEntityPage: Page<LegalEntity>): List<LegalEntityGateInputResponse> {
        return legalEntityPage.content.map { legalEntity ->
            legalEntity.LegalEntityGateInputResponse(legalEntity)
        }
    }

}

private fun toValidSingleLegalEntity(legalEntity: LegalEntity): LegalEntityGateInputResponse {

    return LegalEntityGateInputResponse(
        legalEntity = legalEntity.toLegalEntityDto(),
        legalAddress = legalEntity.legalAddress.toLogisticAddressDto(),
        externalId = legalEntity.externalId
    )
}