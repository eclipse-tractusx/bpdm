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
package org.eclipse.tractusx.bpdm.gate.model.sharingstate

/**
 * Why sharing a business partner input as a golden record failed.
 */
enum class BusinessPartnerSharingError {
    SharingProcessError,
    SharingTimeout,
    BpnNotInPool,
    MissingTaskID,
    NaturalPersonError,
    BpnErrorNotFound,
    BpnErrorTooManyOptions,
    MandatoryFieldValidationFailed,
    BlacklistCountryPresent,
    UnknownSpecialCharacters
}
