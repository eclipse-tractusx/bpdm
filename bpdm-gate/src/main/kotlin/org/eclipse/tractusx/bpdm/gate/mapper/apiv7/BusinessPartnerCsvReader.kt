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

package org.eclipse.tractusx.bpdm.gate.mapper.apiv7

import com.opencsv.bean.CsvToBeanBuilder
import org.eclipse.tractusx.bpdm.gate.model.raw.businesspartner.BusinessPartnerCsvRowRaw
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.InputStreamReader

/**
 * Reads the rows of an uploaded business partner CSV file, without judging their content.
 */
@Component
class BusinessPartnerCsvReader {

    /**
     * Returns the rows of [file] in file order, bound by their column headers.
     */
    fun read(file: MultipartFile): List<BusinessPartnerCsvRowRaw> =
        InputStreamReader(file.inputStream).use { reader ->
            CsvToBeanBuilder<BusinessPartnerCsvRowRaw>(reader)
                .withType(BusinessPartnerCsvRowRaw::class.java)
                .withIgnoreLeadingWhiteSpace(true)
                .build()
                .parse()
        }
}
