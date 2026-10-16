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

package org.eclipse.tractusx.bpdm.gate.mapper.gatev7.outbound

import com.opencsv.CSVWriter
import org.eclipse.tractusx.bpdm.gate.model.upsert.input.BusinessPartnerCsvHeader
import org.springframework.core.io.ByteArrayResource
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter

/**
 * Writes the empty CSV file business partners are uploaded with.
 */
@Component
class BusinessPartnerCsvTemplateWriter {

    /**
     * Returns a CSV file that holds only the header line of every uploadable column.
     */
    fun write(): ByteArrayResource {
        val outputStream = ByteArrayOutputStream()
        CSVWriter(OutputStreamWriter(outputStream)).use { it.writeNext(BusinessPartnerCsvHeader.COLUMNS.toTypedArray()) }
        return ByteArrayResource(outputStream.toByteArray())
    }
}
