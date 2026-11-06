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

package org.eclipse.tractusx.bpdm.pool.model.output.address

import org.eclipse.tractusx.bpdm.pool.model.parsed.error.AddressCreateError

/**
 * Result of creating one address of a batch: either the address as stored or every reason the entry was rejected.
 */
sealed interface AddressCreateResult {
    data class Created(val address: AddressRecord) : AddressCreateResult
    data class Rejected(val errors: List<AddressCreateError>) : AddressCreateResult
}
