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

package org.eclipse.tractusx.bpdm.pool.model

import org.eclipse.tractusx.bpdm.pool.entity.LegalEntityDb
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityContentRequest
import org.eclipse.tractusx.bpdm.pool.model.request.LegalEntityHeaderRequest

/**
 * What a write states about one legal entity, together with the legal entity it lands on.
 *
 * The legal entity is absent where the write creates one, which is what tells a parser whether a value the payload
 * leaves unstated has a current value to fall back on.
 */
data class LegalEntityContentWrite(
    val content: LegalEntityContentRequest,
    val existingLegalEntity: LegalEntityDb?
) {
    val headerWrite get() = LegalEntityHeaderWrite(content.header, existingLegalEntity)
}

/** What a write states about one legal entity's header, together with the legal entity it lands on. */
data class LegalEntityHeaderWrite(
    val header: LegalEntityHeaderRequest,
    val existingLegalEntity: LegalEntityDb?
)
