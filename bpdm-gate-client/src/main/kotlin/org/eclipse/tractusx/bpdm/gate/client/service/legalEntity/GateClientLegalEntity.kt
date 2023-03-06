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

package org.eclipse.tractusx.bpdm.gate.client.service.legalEntity

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.eclipse.tractusx.bpdm.gate.client.config.SpringWebClientConfig
import org.eclipse.tractusx.bpdm.gate.dto.*
import org.eclipse.tractusx.bpdm.gate.dto.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.dto.response.TypeMatchResponse
import org.eclipse.tractusx.bpdm.gate.dto.response.ValidationResponse
import org.springdoc.core.annotations.ParameterObject
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory


class GateClientLegalEntity(webClient: WebClient) {

    private val springWebClientConfig = SpringWebClientConfig(webClient)
    private val client = springWebClientConfig.httpServiceProxyFactory.createClient(GateClientLegalEntityInterface::class.java)


    fun upsertLegalEntities(legalEntities: Collection<LegalEntityGateInput>): ResponseEntity<Any> {
        return client.upsertLegalEntities(legalEntities)
    }

    fun getLegalEntityByExternalId(externalId: String): LegalEntityGateInput {
        return client.getLegalEntityByExternalId(externalId)
    }

    fun getLegalEntities(paginationRequest: PaginationStartAfterRequest): PageStartAfterResponse<LegalEntityGateInput> {
        return client.getLegalEntities(paginationRequest)
    }

    fun getLegalEntitiesOutput(
        paginationRequest: PaginationStartAfterRequest,
        externalIds: Collection<String>?
    ): PageStartAfterResponse<LegalEntityGateOutput> {
        return client.getLegalEntitiesOutput(paginationRequest, externalIds)
    }

    fun validateLegalEntity(legalEntityInput: LegalEntityGateInput): ValidationResponse {
        return client.validateLegalEntity(legalEntityInput)
    }

}