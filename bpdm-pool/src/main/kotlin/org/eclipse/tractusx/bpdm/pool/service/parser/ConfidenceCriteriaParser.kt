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

package org.eclipse.tractusx.bpdm.pool.service.parser

import org.eclipse.tractusx.bpdm.common.model.ParseResult
import org.eclipse.tractusx.bpdm.pool.model.parsed.ConfidenceCriteriaParsed
import org.eclipse.tractusx.bpdm.pool.model.request.ConfidenceCriteriaRequest
import org.springframework.stereotype.Service

/**
 * Validates the confidence assessment a partner states.
 *
 * Every kind of partner states one, and a rejection has to say which partner it is about, so the caller supplies the
 * error rather than the parser declaring one.
 */
@Service
class ConfidenceCriteriaParser {

    /**
     * Reports the assessment as stated, or [incomplete] where it leaves any of its fields unstated.
     */
    fun <E> parse(request: ConfidenceCriteriaRequest, incomplete: E): ParseResult<ConfidenceCriteriaParsed, E> {
        val sharedByOwner = request.sharedByOwner
        val checkedByExternalDataSource = request.checkedByExternalDataSource
        val lastConfidenceCheckAt = request.lastConfidenceCheckAt
        val nextConfidenceCheckAt = request.nextConfidenceCheckAt

        if (sharedByOwner == null || checkedByExternalDataSource == null ||
            lastConfidenceCheckAt == null || nextConfidenceCheckAt == null
        ) return ParseResult.ofSingleFailure(incomplete)

        return ParseResult.Success(
            ConfidenceCriteriaParsed(sharedByOwner, checkedByExternalDataSource, lastConfidenceCheckAt, nextConfidenceCheckAt)
        )
    }
}
