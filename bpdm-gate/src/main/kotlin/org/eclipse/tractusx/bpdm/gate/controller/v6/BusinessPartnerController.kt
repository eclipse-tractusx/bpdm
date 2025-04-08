/*******************************************************************************
 * Copyright (c) 2021,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.gate.controller.v6

import org.eclipse.tractusx.bpdm.common.dto.PageDto
import org.eclipse.tractusx.bpdm.common.dto.PaginationRequest
import org.eclipse.tractusx.bpdm.common.service.toPageRequest
import org.eclipse.tractusx.bpdm.gate.api.v6.GateBusinessPartnerApi
import org.eclipse.tractusx.bpdm.gate.api.v6.model.response.BusinessPartnerOutputDto
import org.eclipse.tractusx.bpdm.gate.config.PermissionConfigProperties
import org.eclipse.tractusx.bpdm.gate.util.getCurrentUserBpn
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController

@RestController("businessPartnerControllerLegacy")
class BusinessPartnerController(
    val businessPartnerLegacyMapper: BusinessPartnerLegacyMapper,
) : GateBusinessPartnerApi {

    @PreAuthorize("hasAuthority(${PermissionConfigProperties.READ_OUTPUT_PARTNER})")
    override fun getBusinessPartnersOutput(
        externalIds: Collection<String>?,
        paginationRequest: PaginationRequest
    ): PageDto<BusinessPartnerOutputDto> {
        return businessPartnerLegacyMapper.getBusinessPartnersOutput(paginationRequest.toPageRequest(), externalIds,  getCurrentUserBpn())
    }
}