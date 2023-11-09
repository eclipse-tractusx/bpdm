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

package org.eclipse.tractusx.bpdm.pool.service

import org.assertj.core.api.Assertions
import org.eclipse.tractusx.bpdm.pool.api.model.response.ErrorInfo
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityCreateError
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityCreateError.*
import org.eclipse.tractusx.bpdm.pool.api.model.response.LegalEntityCreateError.LegalEntityDuplicateIdentifier
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerFullDto
import org.eclipse.tractusx.orchestrator.api.model.BusinessPartnerGenericDto
import org.eclipse.tractusx.orchestrator.api.model.TaskStepReservationEntryDto
import org.junit.jupiter.api.Test

class RequestValidationServiceTest {

    @Test
    fun mergeErrorMapsTest() {

        val request1 =  newRequest("Task1")
        val request2 =  newRequest("Task2")
        val request3 =  newRequest("Task3")

        val firstMap = mapOf(
            request1 to errorInfos(LegalEntityDuplicateIdentifier, LegalFormNotFound),
            request2 to errorInfos(LegalAddressDuplicateIdentifier,LegalFormNotFound, LegalEntityDuplicateIdentifier))
        val secondMap = mapOf(
            request1 to errorInfos(LegalAddressIdentifierNotFound, LegalAddressRegionNotFound),
            request3 to errorInfos(LegalAddressIdentifierNotFound,LegalEntityDuplicateIdentifier))

        val result = RequestValidationService.mergeErrorMaps(firstMap, secondMap)
        Assertions.assertThat(result[request1]!!.size).isEqualTo(4)
        Assertions.assertThat(result[request2]!!.size).isEqualTo(3)
        Assertions.assertThat(result[request3]!!.size).isEqualTo(2)
    }

    private fun errorInfos(vararg codes: LegalEntityCreateError) = codes.map { errorInfo(it) }

    private fun errorInfo(code: LegalEntityCreateError) = ErrorInfo(errorCode = code, message = "message" + code.name, entityKey = "")

    private fun newRequest(taskId: String) =
        TaskStepReservationEntryDto(taskId = taskId, businessPartner = BusinessPartnerFullDto(BusinessPartnerGenericDto()))
}