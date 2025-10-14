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

package org.eclipse.tractusx.bpdm.pool.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.eclipse.tractusx.bpdm.common.model.BaseEntity

@Entity
@Table(
    name = "import_entries",
    indexes = [
        Index(columnList = "import_id"),
        Index(columnList = "bpn")
    ]
)
class ImportEntryDb(
    @Column(name = "import_id", nullable = false, unique = true)
    var importIdentifier: String,
    @Column(name = "bpn", nullable = false)
    var bpn: String
) : BaseEntity()