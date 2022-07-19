package org.eclipse.tractusx.bpdm.common.dto.cdq

data class PartnerProfileCdq(
    val minorityIndicator: WrappedValueCdq? = null,
    val classifications: Collection<ClassificationCdq> = emptyList(),
    val phoneNumbers: Collection<PhoneNumberCdq> = emptyList(),
    val websites: Collection<WebsiteCdq> = emptyList(),
    val contactEmails: Collection<WrappedValueCdq> = emptyList()
)
