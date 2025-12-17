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

package org.eclipse.tractusx.bpdm.gate.v6.auth

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.PostSharingStateReadyRequest
import org.eclipse.tractusx.bpdm.gate.api.v6.client.SharingStateApiClientV6
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.SharingStateDto
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.junit.jupiter.api.Test

class SharingStateAuthV6IT: GateAuthV6Test(), SharingStateApiClientV6 {

    @Test
    fun postSharingStateReady(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Forbidden,
            outputConsumer = AuthExpectationType.Forbidden
        ){
            postSharingStateReady(PostSharingStateReadyRequest(emptyList()))
        }
    }

    @Test
    fun getSharingStates(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Authorized
        ){
            getSharingStates(PaginationRequest(), emptyList())
        }
    }

    override fun postSharingStateReady(request: PostSharingStateReadyRequest) {
       gateClient.sharingStates.postSharingStateReady(request)
    }

    override fun getSharingStates(
        paginationRequest: PaginationRequest,
        externalIds: Collection<String>?
    ): PageDto<SharingStateDto> {
        return gateClient.sharingStates.getSharingStates(paginationRequest, externalIds)
    }
}