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

import java.time.LocalDate

/**
 * Whether this relation counts on [date]: one of its validity periods must cover the date, so a relation without any
 * period never counts. Shared by everything that walks the ownership graph, so they all agree on which edges exist.
 */
fun RelationDb.isValidOn(date: LocalDate): Boolean =
    validityPeriods.any { period -> date >= period.validFrom && (period.validTo == null || date <= period.validTo) }
