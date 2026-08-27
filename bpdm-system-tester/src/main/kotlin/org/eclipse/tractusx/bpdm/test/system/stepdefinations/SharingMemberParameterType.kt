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

package org.eclipse.tractusx.bpdm.test.system.stepdefinations

import io.cucumber.java.ParameterType
import org.eclipse.tractusx.bpdm.test.system.utils.SharingMember

/**
 * Lets a step name the sharing member that acts in it, so one step definition serves them all.
 *
 * A scenario naming no sharing member acts as the first one, which is why the unqualified steps have no
 * sharing member to name.
 */
class SharingMemberParameterType : SpringTestRunConfiguration() {

    @ParameterType("first|second|third")
    fun sharingMember(name: String): SharingMember = SharingMember.valueOf(name.uppercase())
}
