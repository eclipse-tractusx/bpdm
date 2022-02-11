package com.catenax.gpdm.adapter.cdq.dto

data class PartnerProfileCdq(
    val minorityIndicator: WrappedValueCdq?,
    val classifications: Collection<ClassificationCdq> = emptyList(),
    val phoneNumbers: Collection<PhoneNumberCdq> = emptyList(),
    val websites: Collection<WebsiteCdq> = emptyList(),
    val contactEmails: Collection<WrappedValueCdq> = emptyList()
    )
