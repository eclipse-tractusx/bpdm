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

package org.eclipse.tractusx.bpdm.common.mapping

import com.neovisionaries.i18n.LanguageCode
import org.springframework.stereotype.Component

@Component
class BpdmLanguageCodeMapper: BpdmStringMapper<LanguageCode> {

    private val entriesByName = LanguageCode.entries.associateBy { it.name }

    override fun map(valueToMap: String, context: ValidationContext): MappingResult<LanguageCode> {
        return MappingResult.errorOnNull(entriesByName[valueToMap]){
            listOf(CommonValidationErrorCodes.ISO6391.toValidationError(valueToMap, context))
        }
    }
}