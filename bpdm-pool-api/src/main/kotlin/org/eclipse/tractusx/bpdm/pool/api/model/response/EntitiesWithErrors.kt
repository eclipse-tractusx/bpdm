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

package org.eclipse.tractusx.bpdm.pool.api.model.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.LogisticAddressResponse

@Schema(name = "EntitiesWithErrorsResponse", description = "Holds information about successfully and failed entities after the creating/updating of several objects")

data class EntitiesWithErrors<ENTITY, out ERROR : ErrorCode>(

    @Schema(description = "Successfully created entities")
    val entities: Collection<ENTITY>,
    @Schema(description = "Errors for not created entities")
    val errors: Collection<ErrorInfo<ERROR>>
) {
    val entityCount: Int
        get() = entities.size
    val errorCount: Int
        get() = errors.size
}

typealias LegalEntityPartnerCreateResponseWrapper = EntitiesWithErrors<LegalEntityPartnerCreateResponse, LegalEntityCreateError>
typealias LegalEntityPartnerUpdateResponseWrapper = EntitiesWithErrors<LegalEntityPartnerCreateResponse, LegalEntityUpdateError>
typealias SitePartnerCreateResponseWrapper = EntitiesWithErrors<SitePartnerCreateResponse, SiteCreateError>
typealias SitePartnerUpdateResponseWrapper = EntitiesWithErrors<SitePartnerCreateResponse, SiteUpdateError>
typealias AddressPartnerCreateResponseWrapper = EntitiesWithErrors<AddressPartnerCreateResponse, AddressCreateError>
typealias AddressPartnerUpdateResponseWrapper = EntitiesWithErrors<LogisticAddressResponse, AddressUpdateError>
