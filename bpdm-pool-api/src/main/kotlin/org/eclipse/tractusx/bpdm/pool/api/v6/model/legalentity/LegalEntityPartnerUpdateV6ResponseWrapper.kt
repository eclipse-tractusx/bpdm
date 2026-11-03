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

package org.eclipse.tractusx.bpdm.pool.api.v6.model.legalentity

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.openapidescription.CommonDescription
import org.eclipse.tractusx.bpdm.pool.api.v6.exception.LegalEntityUpdateErrorV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.error.EntitiesWithErrorsV6
import org.eclipse.tractusx.bpdm.pool.api.v6.model.error.ErrorInfoV6

@Schema(description = CommonDescription.headerEntityWithErrorsWrapper, deprecated = true)
data class LegalEntityPartnerUpdateV6ResponseWrapper(
    override val entities: Collection<LegalEntityPartnerCreateVerboseV6Dto>,
    override val errors: Collection<ErrorInfoV6<LegalEntityUpdateErrorV6>>
) : EntitiesWithErrorsV6<LegalEntityPartnerCreateVerboseV6Dto, LegalEntityUpdateErrorV6>(entities, errors)

