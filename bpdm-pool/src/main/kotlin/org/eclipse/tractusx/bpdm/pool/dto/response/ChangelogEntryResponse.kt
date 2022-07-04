package org.eclipse.tractusx.bpdm.pool.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.pool.entity.ChangelogType
import java.time.Instant

@Schema(name = "Changelog Entry Response", description = "Changelog entry for a business partner")
data class ChangelogEntryResponse(
    @Schema(description = "Business Partner Number of the changelog entry")
    val bpn: String,
    @Schema(description = "The type of the change")
    val changelogType: ChangelogType,
    @Schema(description = "The timestamp of the change")
    val timestamp: Instant
)
