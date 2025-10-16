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

package org.eclipse.tractusx.bpdm.pool.v6.sharingmember.auth

import org.eclipse.tractusx.bpdm.pool.v6.auth.MetadataApiAuthV6Test
import org.eclipse.tractusx.bpdm.pool.v6.sharingmember.SharingMemberTest
import org.eclipse.tractusx.bpdm.test.util.AuthExpectationType

class MetadataApiSharingMemberAuthV6Test: SharingMemberTest(), MetadataApiAuthV6Test {
    override val expectationCreateIdentifierType = AuthExpectationType.Forbidden
    override val expectationGetIdentifierTypes = AuthExpectationType.Authorized
    override val expectationCreateLegalForm = AuthExpectationType.Forbidden
    override val expectationGetLegalForms = AuthExpectationType.Authorized
}