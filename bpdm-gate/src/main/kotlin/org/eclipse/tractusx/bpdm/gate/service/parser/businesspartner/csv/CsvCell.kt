/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.bpdm.gate.service.parser.businesspartner.csv

/**
 * A cell of an uploaded CSV row together with its column, in which an empty cell reads as absent.
 */
data class CsvCell(val column: String, private val content: String?) {
    val value: String? = cellText(content)
}

/**
 * Returns the text of a CSV cell, absent for an empty cell.
 */
fun cellText(content: String?): String? = content?.takeIf { it.isNotEmpty() }
