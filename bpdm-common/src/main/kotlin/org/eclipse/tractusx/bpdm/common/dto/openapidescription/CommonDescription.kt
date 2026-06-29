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

package org.eclipse.tractusx.bpdm.common.dto.openapidescription

object CommonDescription {
    const val headerEntityWithErrorsWrapper = "Holds information about successfully and failed entities after the creating/updating of several objects"

    const val createdAt = "The date when the data record has been created."
    const val updatedAt = "The date when the data record has been last updated."

    const val index = "User defined index to conveniently match this entry to the corresponding entry in the response."
    const val score = "Relative quality score of the match. The higher the better."

    const val externalId = "The identifier which uniquely identifies (in the internal system landscape of the sharing member) the business partner."

    const val roles = "Roles this business partner takes in relation to the sharing member."

    const val ownershipUltimate = "Whether this legal entity is the ultimate owner in the ownership chain. \" +\n" +
            "            \"This flag is provided by the golden record process and persisted in the Pool. \" +\n" +
            "            \"The ultimate owner resolution logic is not implemented in this feature."

    const  val  ultimateOwnerBpnl =   "The BPNL of the ultimate owner in the ownership chain. " +
            "This field is persisted in the Pool but not resolved in this feature. " +
            "It will be populated by future features that implement ultimate owner resolution."

}