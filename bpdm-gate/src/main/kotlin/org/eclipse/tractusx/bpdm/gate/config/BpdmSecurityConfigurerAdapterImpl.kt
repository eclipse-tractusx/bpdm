package org.eclipse.tractusx.bpdm.gate.config

import org.eclipse.tractusx.bpdm.common.config.BpdmSecurityConfigurerAdapter
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy

@Configuration
class BpdmSecurityConfigurerAdapterImpl : BpdmSecurityConfigurerAdapter {
    override fun configure(http: HttpSecurity) {
        http.csrf().disable()
            .cors()
            .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and().authorizeRequests()
            .antMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
            .antMatchers("/v3/api-docs/**").permitAll()
            .antMatchers("/api/swagger-ui/**").permitAll()
            .antMatchers(HttpMethod.GET, "/api/**").authenticated()
            .antMatchers("/api/**").hasRole("add_company_data")
    }
}