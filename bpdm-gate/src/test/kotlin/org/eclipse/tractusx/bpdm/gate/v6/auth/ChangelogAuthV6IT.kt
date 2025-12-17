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

import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.gate.api.model.request.ChangelogSearchRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.ChangelogGateDto
import org.eclipse.tractusx.bpdm.gate.api.model.response.PageChangeLogDto
import org.eclipse.tractusx.bpdm.gate.api.v6.client.ChangelogApiClientV6
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType
import org.junit.jupiter.api.Test

class ChangelogAuthV6IT: GateAuthV6Test(), ChangelogApiClientV6 {

    @Test
    fun getInputChangelog(){
        assertExpectations(
            inputManager = AuthExpectationType.Authorized,
            inputConsumer = AuthExpectationType.Authorized,
            outputConsumer = AuthExpectationType.Forbidden
        ){
            getInputChangelog(PaginationRequest(), ChangelogSearchRequest())
        }
    }

    @Test
    fun getOutputChangelog(){
        assertExpectations(
            inputManager = AuthExpectationType.Forbidden,
            inputConsumer = AuthExpectationType.Forbidden,
            outputConsumer = AuthExpectationType.Authorized
        ){
            getOutputChangelog(PaginationRequest(), ChangelogSearchRequest())
        }
    }

    override fun getInputChangelog(
        paginationRequest: PaginationRequest,
        searchRequest: ChangelogSearchRequest
    ): PageChangeLogDto<ChangelogGateDto> {
       return gateClient.changelog.getInputChangelog(paginationRequest, searchRequest)
    }

    override fun getOutputChangelog(
        paginationRequest: PaginationRequest,
        searchRequest: ChangelogSearchRequest
    ): PageChangeLogDto<ChangelogGateDto> {
       return gateClient.changelog.getOutputChangelog(paginationRequest, searchRequest)
    }
}