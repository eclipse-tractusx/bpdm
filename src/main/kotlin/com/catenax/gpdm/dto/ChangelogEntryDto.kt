package com.catenax.gpdm.dto

import com.catenax.gpdm.entity.ChangelogType

data class ChangelogEntryDto(val bpn: String, val changelogType: ChangelogType)