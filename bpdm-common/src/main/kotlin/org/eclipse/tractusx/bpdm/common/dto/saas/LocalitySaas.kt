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

package org.eclipse.tractusx.bpdm.common.dto.saas

import org.eclipse.tractusx.bpdm.common.model.SaasLocalityType
import org.eclipse.tractusx.bpdm.common.model.toSaasTypeDto

data class LocalitySaas(
    override val type: TypeKeyNameUrlSaas? = null,
    val shortName: String? = null,
    override val value: String? = null,
    val language: LanguageSaas? = null
) : TypeValueSaas {
    constructor(saasValue: String, saasType: SaasLocalityType, saasLanguage: LanguageSaas?)
            : this(value = saasValue, type = saasType.toSaasTypeDto(), language = saasLanguage)
}
