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

package org.eclipse.tractusx.bpdm.pool.service.validation.validator.impl

import org.eclipse.tractusx.bpdm.pool.dto.validation.Validated
import org.eclipse.tractusx.bpdm.pool.dto.validation.ValidatedOptional
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.IsMissingError
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.NoMatchError
import org.eclipse.tractusx.bpdm.pool.dto.validation.error.RequiredMatchError
import org.springframework.stereotype.Service

@Service
class MatchValidator {

    fun <T, R> matchNullable(keys: List<T?>, foundByKey: Map<T, R>): List<ValidatedOptional<R, NoMatchError>>{
        return keys.map { key ->
            if(key == null) return@map ValidatedOptional(null, emptySet())

            val foundKey = foundByKey[key]
            val errors = if(foundKey == null) setOf(NoMatchError()) else setOf()

            ValidatedOptional(foundKey, errors)
        }
    }

    fun <T, R> matchRequired(keys: List<T?>, foundByKey: Map<T, R>): List<Validated<R, RequiredMatchError>>{
        return keys.map { key ->
            if(key == null) return@map Validated(null, setOf(RequiredMatchError.IsMissing(IsMissingError())))

            val foundKey = foundByKey[key]
            val errors = if(foundKey == null) setOf(RequiredMatchError.NoMatch(NoMatchError())) else setOf()

            Validated(foundKey, errors)
        }
    }
}