

package org.eclipse.tractusx.bpdm.pool.model.parsed.site

/**
 * Site search holding normalized site and legal entity BPNs and a non-blank name.
 */
data class SiteSearchParsed(
    val siteBpns: List<String>,
    val legalEntityBpns: List<String>,
    val name: String?,
    val isDataSpaceParticipant: Boolean?
)
