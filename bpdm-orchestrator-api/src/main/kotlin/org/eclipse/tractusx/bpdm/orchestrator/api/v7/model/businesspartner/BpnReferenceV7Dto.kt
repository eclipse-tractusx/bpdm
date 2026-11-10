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
 * SPDX-License-IdentifierV7Dto: Apache-2.0
 ******************************************************************************/

package org.eclipse.tractusx.bpdm.orchestrator.api.v7.model.businesspartner

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A reference to a BPN for the corresponding business partner data. " +
        "Either this reference contains an existing BPN or a BPN request identifier. " +
        "The golden record process makes sure that the each unique BPN request identifier is associated to a unique BPN. " +
        "For a new BPN request identifier the golden record process creates a new BPN (and golden record) and associates it with the BPN request identifier. " +
        "Known BPN request identifiers are resolved to the BPN and the corresponding golden record is updated with the business partner data. " +
        "This makes it possible for duplication check services to reference unique business partners by their own BPN request identifiers instead of needing to rely on BPNs for identification.")
data class BpnReferenceV7Dto(
    @Schema(description =  "The value of the reference, either an existing BPN or a BPN request identifier (which is freely chosen by the duplication check provider)")
    val referenceValue: String?,
    @Schema(description = "The desired BPN when a new golden record has to be created for this business partner data." +
            "Requires a BPN request identifier that is not associated with an existing BPN. " +
            "At this moment the golden record process does not support creating desired BPNs and ignores this field. ")
    val desiredBpn: String?,
    @Schema(description =  "Whether this reference is a BPN or BPN request identifier")
    val referenceType: BpnReferenceTypeV7?
){
    companion object{
        val empty = BpnReferenceV7Dto(null, null, null)
    }
}
