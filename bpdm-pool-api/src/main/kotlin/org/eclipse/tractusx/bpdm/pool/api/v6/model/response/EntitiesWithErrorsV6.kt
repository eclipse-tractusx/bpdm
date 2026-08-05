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

package org.eclipse.tractusx.bpdm.pool.api.v6.model.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.CommonDescription
import org.eclipse.tractusx.bpdm.pool.api.v6.model.LogisticAddressVerboseDtoV6

@Schema(deprecated = true)
open class EntitiesWithErrorsV6<ENTITY, out ERROR : ErrorCodeV6>(

    @Schema(description = "Successfully created entities")
    open val entities: Collection<ENTITY>,
    @Schema(description = "Errors for not created entities")
    open val errors: Collection<ErrorInfoV6<ERROR>>
)

@Schema(description = CommonDescription.headerEntityWithErrorsWrapper, deprecated = true)
data class LegalEntityPartnerCreateResponseWrapperV6(
    override val entities: Collection<LegalEntityPartnerCreateVerboseDtoV6>,
    override val errors: Collection<ErrorInfoV6<LegalEntityCreateErrorV6>>
) : EntitiesWithErrorsV6<LegalEntityPartnerCreateVerboseDtoV6, LegalEntityCreateErrorV6>(entities, errors)

@Schema(description = CommonDescription.headerEntityWithErrorsWrapper, deprecated = true)
data class LegalEntityPartnerUpdateResponseWrapperV6(
    override val entities: Collection<LegalEntityPartnerCreateVerboseDtoV6>,
    override val errors: Collection<ErrorInfoV6<LegalEntityUpdateErrorV6>>
) : EntitiesWithErrorsV6<LegalEntityPartnerCreateVerboseDtoV6, LegalEntityUpdateErrorV6>(entities, errors)

@Schema(description = CommonDescription.headerEntityWithErrorsWrapper, deprecated = true)
data class SitePartnerCreateResponseWrapperV6(
    override val entities: Collection<SitePartnerCreateVerboseDtoV6>,
    override val errors: Collection<ErrorInfoV6<SiteCreateErrorV6>>
) : EntitiesWithErrorsV6<SitePartnerCreateVerboseDtoV6, SiteCreateErrorV6>(entities, errors)

@Schema(description = CommonDescription.headerEntityWithErrorsWrapper, deprecated = true)
data class SitePartnerUpdateResponseWrapperV6(
    override val entities: Collection<SitePartnerCreateVerboseDtoV6>,
    override val errors: Collection<ErrorInfoV6<SiteUpdateErrorV6>>
) : EntitiesWithErrorsV6<SitePartnerCreateVerboseDtoV6, SiteUpdateErrorV6>(entities, errors)

@Schema(description = CommonDescription.headerEntityWithErrorsWrapper, deprecated = true)
data class AddressPartnerCreateResponseWrapperV6(
    override val entities: Collection<AddressPartnerCreateVerboseDtoV6>,
    override val errors: Collection<ErrorInfoV6<AddressCreateErrorV6>>
) : EntitiesWithErrorsV6<AddressPartnerCreateVerboseDtoV6, AddressCreateErrorV6>(entities, errors)

@Schema(description = CommonDescription.headerEntityWithErrorsWrapper, deprecated = true)
data class AddressPartnerUpdateResponseWrapperV6(
    override val entities: Collection<LogisticAddressVerboseDtoV6>,
    override val errors: Collection<ErrorInfoV6<AddressUpdateErrorV6>>
) : EntitiesWithErrorsV6<LogisticAddressVerboseDtoV6, AddressUpdateErrorV6>(entities, errors)
