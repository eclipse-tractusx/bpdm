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

package org.eclipse.tractusx.bpdm.gate.client.service.address

import org.eclipse.tractusx.bpdm.gate.client.config.SpringWebClientConfig
import org.eclipse.tractusx.bpdm.gate.dto.AddressGateInput
import org.eclipse.tractusx.bpdm.gate.dto.AddressGateOutput
import org.eclipse.tractusx.bpdm.gate.dto.request.PaginationStartAfterRequest
import org.eclipse.tractusx.bpdm.gate.dto.response.PageStartAfterResponse
import org.eclipse.tractusx.bpdm.gate.dto.response.ValidationResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient


class GateClientAddress(webClient: WebClient) {

    private val springWebClientConfig = SpringWebClientConfig(webClient)
    private val client = springWebClientConfig.httpServiceProxyFactory.createClient(GateClientAddressInterface::class.java)

    fun getAddressByExternalId (externalId: String): AddressGateInput {
        return client.getAddressByExternalId(externalId)
    }

    fun upsertAddresses (addresses: Collection<AddressGateInput>): ResponseEntity<Any> {
        return client.upsertAddresses(addresses)
    }

    fun getAddresses(paginationRequest: PaginationStartAfterRequest): PageStartAfterResponse<AddressGateInput> {
        return client.getAddresses(paginationRequest)
    }

    fun getAddressesOutput(
        paginationRequest: PaginationStartAfterRequest,
        externalIds: Collection<String>?
    ): PageStartAfterResponse<AddressGateOutput> {
        return client.getAddressesOutput(paginationRequest,externalIds)
    }

    fun validateSite(addressInput: AddressGateInput): ValidationResponse {
        return client.validateSite(addressInput)
    }

}