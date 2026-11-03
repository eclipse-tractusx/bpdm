/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.model.output.metadata

import org.eclipse.tractusx.bpdm.pool.model.parsed.metadata.IdentifierTypeSearchError
import org.springframework.data.domain.Page

/**
 * Result of an identifier type search: either the requested page or every reason the criteria were rejected.
 */
sealed interface IdentifierTypeSearchResult {
    data class Found(val page: Page<IdentifierTypeRecord>) : IdentifierTypeSearchResult
    data class Rejected(val errors: List<IdentifierTypeSearchError>) : IdentifierTypeSearchResult
}
