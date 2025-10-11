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

package org.eclipse.tractusx.bpdm.pool.v6.anonymous

import org.eclipse.tractusx.bpdm.pool.v6.auth.AddressApiAuthV6Test
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType

class AddressApiAnonymousAuthV6Test : AnonymousTest(), AddressApiAuthV6Test {
    override val expectationGetAddresses = AuthExpectationType.Unauthorized
    override val expectationGetAddress = AuthExpectationType.Unauthorized
    override val expectationSearchAddresses = AuthExpectationType.Unauthorized
    override val expectationCreateAddresses = AuthExpectationType.Unauthorized
    override val expectationUpdateAddresses = AuthExpectationType.Unauthorized
}