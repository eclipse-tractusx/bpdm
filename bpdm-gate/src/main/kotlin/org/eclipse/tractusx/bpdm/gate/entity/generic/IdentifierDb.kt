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

package org.eclipse.tractusx.bpdm.gate.entity.generic

import jakarta.persistence.Column
import jakarta.persistence.Embeddable


@Embeddable
data class IdentifierDb(

    @Column(name = "type", nullable = false)
    var type: String,

    @Column(name = "value", nullable = false)
    var value: String,

    @Column(name = "issuing_body")
    var issuingBody: String?

) : Comparable<IdentifierDb> {

    //  Natural order by "type", "value", "issuingBody"
    override fun compareTo(other: IdentifierDb) = compareBy(
        IdentifierDb::type,
        IdentifierDb::value,
        IdentifierDb::issuingBody
    ).compare(this, other)
}
