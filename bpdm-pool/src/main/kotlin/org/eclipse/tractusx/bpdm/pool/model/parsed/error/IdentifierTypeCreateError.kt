/*******************************************************************************
 * Copyright (c) 2021,2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.pool.model.parsed.error

import org.eclipse.tractusx.bpdm.pool.api.v7.model.metadata.IdentifierBusinessPartnerTypeV7

/**
 * Reason an identifier type create is rejected.
 */
sealed interface IdentifierTypeCreateError {
    data class CountryNotFound(val country: String) : IdentifierTypeCreateError
    data class TechnicalKeyAlreadyTaken(
        val technicalKey: String,
        val businessPartnerType: IdentifierBusinessPartnerTypeV7
    ) : IdentifierTypeCreateError
}
