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

package org.eclipse.tractusx.bpdm.test.system.utils

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.time.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.client.GateClient
import org.eclipse.tractusx.bpdm.gate.api.model.ConfidenceCriteriaDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType
import org.eclipse.tractusx.bpdm.gate.api.model.response.AddressComponentOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.LegalEntityRepresentationOutputDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.SiteRepresentationOutputDto
import java.time.Duration

class StepUtils(
    private val gateClient: GateClient
) {

    fun waitForBusinessPartnerResult(externalId: String): SharingStateType = runBlocking {
        println("Waiting for business partner result for $externalId ...")
        withTimeout(Duration.ofMinutes(4)) {
            while (true) {
                val sharingState = gateClient.sharingState.getSharingStates(PaginationRequest(), listOf(externalId)).content.single()
                if (sharingState.sharingStateType == SharingStateType.Success || sharingState.sharingStateType == SharingStateType.Error) {
                    return@withTimeout sharingState.sharingStateType
                }
                delay(Duration.ofSeconds(10))
            }
        } as SharingStateType
    }

    fun waitForBusinessPartnerResultConfidenceSync(externalId: String): BusinessPartnerOutputDto = runBlocking {
        println("Waiting for business partner result confidences to stabilize for $externalId ...")
        withTimeout(Duration.ofMinutes(2)) {
            while (true) {
                val currentOutput = gateClient.businessParters.getBusinessPartnersOutput(listOf(externalId), PaginationRequest()).content.single()
                if(currentOutput.address.confidenceCriteria.numberOfSharingMembers == 1){
                    return@withTimeout currentOutput
                }
                delay(Duration.ofSeconds(10))
            }
        } as BusinessPartnerOutputDto
    }


    fun waitForRelationResult(externalId: String): RelationSharingStateType = runBlocking {
        println("Waiting for relation result for $externalId ...")
        withTimeout(Duration.ofMinutes(4)) {
            while (true) {
                val sharingState = gateClient.relationSharingState.get(externalIds = listOf(externalId)).content.single()
                if (sharingState.sharingStateType == RelationSharingStateType.Success || sharingState.sharingStateType == RelationSharingStateType.Error) {
                    return@withTimeout sharingState.sharingStateType
                }
                delay(Duration.ofSeconds(10))
            }
        } as RelationSharingStateType
    }

    fun waitForRelationTask(externalId: String): String = runBlocking {
        println("Waiting relation getting task assigned for $externalId ...")
        withTimeout(Duration.ofMinutes(4)) {
            while (true) {
                val sharingState = gateClient.relationSharingState.get(externalIds = listOf(externalId)).content.single()
                if(sharingState.taskId != null)
                    return@withTimeout sharingState.taskId
                delay(Duration.ofSeconds(10))
            }
        } as String
    }


    fun assertEqualIgnoreBpns(actualOutput: BusinessPartnerOutputDto, expectedOutput: BusinessPartnerOutputDto){
        assertThat(actualOutput)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFields(*ignoredFields)
            .isEqualTo(expectedOutput)
    }

    val ignoredFields = arrayOf(
        BusinessPartnerOutputDto::createdAt.name,
        BusinessPartnerOutputDto::updatedAt.name,
        "${BusinessPartnerOutputDto::legalEntity.name}.${LegalEntityRepresentationOutputDto::legalEntityBpn.name}",
        "${BusinessPartnerOutputDto::site.name}.${SiteRepresentationOutputDto::siteBpn.name}",
        "${BusinessPartnerOutputDto::address.name}.${AddressComponentOutputDto::addressBpn.name}",
        // ToDo: Cleaning service dummy should have fixed confidence criteria dummy times otherwise we need to keep ignoring these fields
        "${BusinessPartnerOutputDto::legalEntity.name}.${AddressComponentOutputDto::confidenceCriteria.name}.${ConfidenceCriteriaDto::lastConfidenceCheckAt.name}",
        "${BusinessPartnerOutputDto::legalEntity.name}.${AddressComponentOutputDto::confidenceCriteria.name}.${ConfidenceCriteriaDto::nextConfidenceCheckAt.name}",
        "${BusinessPartnerOutputDto::site.name}.${AddressComponentOutputDto::confidenceCriteria.name}.${ConfidenceCriteriaDto::lastConfidenceCheckAt.name}",
        "${BusinessPartnerOutputDto::site.name}.${AddressComponentOutputDto::confidenceCriteria.name}.${ConfidenceCriteriaDto::nextConfidenceCheckAt.name}",
        "${BusinessPartnerOutputDto::address.name}.${AddressComponentOutputDto::confidenceCriteria.name}.${ConfidenceCriteriaDto::lastConfidenceCheckAt.name}",
        "${BusinessPartnerOutputDto::address.name}.${AddressComponentOutputDto::confidenceCriteria.name}.${ConfidenceCriteriaDto::nextConfidenceCheckAt.name}",
    )


}