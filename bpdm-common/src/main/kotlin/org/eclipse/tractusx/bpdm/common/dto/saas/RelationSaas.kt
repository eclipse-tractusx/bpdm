/*******************************************************************************
 * Copyright (c) 2021,2023 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.bpdm.common.dto.saas

import java.time.LocalDateTime

data class RelationSaas(
    val startedAt: LocalDateTime? = null,
    val endedAt: LocalDateTime? = null,
    val type: TypeKeyNameSaas? = null,
    val relationClass: TypeKeyNameSaas? = null,
    val startNode: String,
    val startNodeDataSource: String,
    val endNode: String,
    val endNodeDataSource: String
)
