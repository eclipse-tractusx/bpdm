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

package org.eclipse.tractusx.bpdm.pool.model

/**
 * A street's script-variant text, shared by the loose request and the validated stage because every one of its parts
 * stays optional in both.
 */
data class StreetScriptVariant(
    val name: String? = null,
    val direction: String? = null,
    val namePrefix: String? = null,
    val additionalNamePrefix: String? = null,
    val nameSuffix: String? = null,
    val additionalNameSuffix: String? = null
)
