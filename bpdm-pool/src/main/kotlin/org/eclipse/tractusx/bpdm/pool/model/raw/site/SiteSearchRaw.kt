

package org.eclipse.tractusx.bpdm.pool.model.raw.site

data class SiteSearchRaw(
    val siteBpns: List<String>,
    val legalEntityBpns: List<String>,
    val name: String?
)
