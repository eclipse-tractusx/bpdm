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
 * A single field's contribution to a partial update: either [Set] it to a value (which, for a nullable field, includes
 * clearing it to `null`) or leave it untouched with [NoOp]. Update DTOs are built from these so a caller can express a
 * full replace (every field [Set]) or a targeted change (only some fields [Set], the rest [NoOp]) with the same shape,
 * and the applying service — not the caller — decides how each [Set] value is written.
 */
sealed interface FieldUpdate<out T> {
    data class Set<out T>(val value: T) : FieldUpdate<T>
    data object NoOp : FieldUpdate<Nothing>
}

/** Runs [action] with the new value only when this field is [FieldUpdate.Set]; a [FieldUpdate.NoOp] is ignored. */
inline fun <T> FieldUpdate<T>.ifSet(action: (T) -> Unit) {
    if (this is FieldUpdate.Set) action(value)
}

/**
 * The new value when this field is [FieldUpdate.Set], otherwise [current]. For fields that cannot be written in
 * isolation — several update fields backing one composite entity value — so an untouched part carries forward.
 */
fun <T> FieldUpdate<T>.orKeep(current: T): T =
    if (this is FieldUpdate.Set) value else current
