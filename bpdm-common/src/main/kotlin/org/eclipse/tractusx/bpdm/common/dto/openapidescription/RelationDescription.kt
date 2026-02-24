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

object RelationDescription {
    const val header = "A relation from one business partner (the source) to another business partner (the target)."

    const val externalId = "The identifier with which to reference this relation"
    const val relationType = "The type of relation between the business partners"
    const val source = "The business partner from which the relation emerges (the source)"
    const val target = "The business partner to which this relation goes (the target)"
    const val reasonCode = "The technical key of the reason code describing why the relation was established. The list of reason codes is available from the golden record Pool."
    const val updatedAt = "The time when this relation was last modified"
    const val createdAt = "The time when this relation was created"
}