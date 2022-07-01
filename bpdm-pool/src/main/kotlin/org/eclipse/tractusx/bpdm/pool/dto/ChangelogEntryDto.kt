package org.eclipse.tractusx.bpdm.pool.dto

import org.eclipse.tractusx.bpdm.pool.entity.ChangelogType

data class ChangelogEntryDto(val bpn: String, val changelogType: ChangelogType)