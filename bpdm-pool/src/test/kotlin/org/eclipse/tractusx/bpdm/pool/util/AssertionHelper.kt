package org.eclipse.tractusx.bpdm.pool.util

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.tractusx.bpdm.pool.dto.response.AddressWithReferenceResponse

object AssertionHelper {

    fun assertAddressWithReferenceEquals(actual: AddressWithReferenceResponse, expected: AddressWithReferenceResponse) {
        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*uuid")
            .ignoringAllOverriddenEquals()
            .ignoringCollectionOrder()
            .isEqualTo(expected)
    }

}