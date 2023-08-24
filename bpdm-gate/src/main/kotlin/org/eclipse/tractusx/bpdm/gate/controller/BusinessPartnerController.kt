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

package org.eclipse.tractusx.bpdm.gate.controller

import org.eclipse.tractusx.bpdm.common.dto.request.PaginationRequest
import org.eclipse.tractusx.bpdm.common.dto.response.PageDto
import org.eclipse.tractusx.bpdm.gate.api.GateBusinessPartnerApi
import org.eclipse.tractusx.bpdm.gate.api.model.request.BusinessPartnerInputRequest
import org.eclipse.tractusx.bpdm.gate.api.model.response.BusinessPartnerInputResponseDto
import org.springframework.web.bind.annotation.RestController

@RestController
class BusinessPartnerController() : GateBusinessPartnerApi {

    override fun upsertBusinessPartnersInput(businessPartners: Collection<BusinessPartnerInputRequest>): Collection<BusinessPartnerInputResponseDto> {
        TODO("Not yet implemented")
    }

    override fun getBusinessPartnersInputByExternalIds(
        paginationRequest: PaginationRequest,
        externalIds: Collection<String>
    ): PageDto<BusinessPartnerInputResponseDto> {
        TODO("Not yet implemented")
    }
}