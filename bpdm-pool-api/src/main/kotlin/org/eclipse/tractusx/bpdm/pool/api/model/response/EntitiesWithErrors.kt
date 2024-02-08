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

package org.eclipse.tractusx.bpdm.pool.api.model.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.CommonDescription
import org.eclipse.tractusx.bpdm.pool.api.model.LogisticAddressVerboseDto

open class EntitiesWithErrors<ENTITY, out ERROR : ErrorCode>(

    @Schema(description = "Successfully created entities")
    open val entities: Collection<ENTITY>,
    @Schema(description = "Errors for not created entities")
    open val errors: Collection<ErrorInfo<ERROR>>
) {
    val entityCount: Int
        get() = entities.size
    val errorCount: Int
        get() = errors.size
}

@Schema(description = CommonDescription.headerEntityWithErrorsWrapper)
data class LegalEntityPartnerCreateResponseWrapper(
    override val entities: Collection<LegalEntityPartnerCreateVerboseDto>,
    override val errors: Collection<ErrorInfo<LegalEntityCreateError>>
) : EntitiesWithErrors<LegalEntityPartnerCreateVerboseDto, LegalEntityCreateError>(entities, errors)

@Schema(description = CommonDescription.headerEntityWithErrorsWrapper)
data class LegalEntityPartnerUpdateResponseWrapper(
    override val entities: Collection<LegalEntityPartnerCreateVerboseDto>,
    override val errors: Collection<ErrorInfo<LegalEntityUpdateError>>
) : EntitiesWithErrors<LegalEntityPartnerCreateVerboseDto, LegalEntityUpdateError>(entities, errors)

@Schema(description = CommonDescription.headerEntityWithErrorsWrapper)
data class SitePartnerCreateResponseWrapper(
    override val entities: Collection<SitePartnerCreateVerboseDto>,
    override val errors: Collection<ErrorInfo<SiteCreateError>>
) : EntitiesWithErrors<SitePartnerCreateVerboseDto, SiteCreateError>(entities, errors)

@Schema(description = CommonDescription.headerEntityWithErrorsWrapper)
data class SitePartnerUpdateResponseWrapper(
    override val entities: Collection<SitePartnerCreateVerboseDto>,
    override val errors: Collection<ErrorInfo<SiteUpdateError>>
) : EntitiesWithErrors<SitePartnerCreateVerboseDto, SiteUpdateError>(entities, errors)

@Schema(description = CommonDescription.headerEntityWithErrorsWrapper)
data class AddressPartnerCreateResponseWrapper(
    override val entities: Collection<AddressPartnerCreateVerboseDto>,
    override val errors: Collection<ErrorInfo<AddressCreateError>>
) : EntitiesWithErrors<AddressPartnerCreateVerboseDto, AddressCreateError>(entities, errors)

@Schema(description = CommonDescription.headerEntityWithErrorsWrapper)
data class AddressPartnerUpdateResponseWrapper(
    override val entities: Collection<LogisticAddressVerboseDto>,
    override val errors: Collection<ErrorInfo<AddressUpdateError>>
) : EntitiesWithErrors<LogisticAddressVerboseDto, AddressUpdateError>(entities, errors)
