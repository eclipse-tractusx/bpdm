package org.eclipse.tractusx.bpdm.common.config

import org.springframework.security.config.annotation.web.builders.HttpSecurity

interface BpdmSecurityConfigurerAdapter {
    fun configure(http: HttpSecurity)
}