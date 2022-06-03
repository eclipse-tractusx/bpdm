package com.catenax.gpdm.config

import mu.withLoggingContext
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.task.TaskDecorator
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
@ConditionalOnProperty(
    value = ["bpdm.logging.show-user"],
    havingValue = "true",
    matchIfMissing = false
)
class UserLoggingFilter(
    private val logConfigProperties: LogConfigProperties
) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val userName = request.userPrincipal?.name ?: logConfigProperties.unknownUser
        val userNameLog = toLogFormat(userName)
        withLoggingContext(
            "user" to userNameLog,
        ) {
            logger.info("User '$userName' requests ${request.method} ${request.requestURI}...")
            filterChain.doFilter(request, response)
            logger.info("Response with status ${response.status}")
        }
    }

    private fun toLogFormat(userName: String): String {
        var logUserName: String = if (userName.length > logConfigProperties.userMaxLength)
            userName.substring(userName.length - logConfigProperties.userMaxLength)
        else
            userName

        logUserName = logUserName.padEnd(logConfigProperties.userMaxLength, ' ')
        return "[$logUserName]"
    }
}

@Component
@ConditionalOnProperty(
    value = ["bpdm.logging.show-request"],
    havingValue = "true",
    matchIfMissing = false
)
class RequestLoggingFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val requestId = UUID.randomUUID().toString()

        withLoggingContext(
            "request" to requestId
        ) {
            filterChain.doFilter(request, response)
        }
    }
}

/**
 * Util class for copying the Mapped Diagnostic Context from an invoking thread to the invoked thread
 */
@Component
class MdcTaskDecorator : TaskDecorator {
    override fun decorate(runnable: Runnable): Runnable {
        val mdcCopy = MDC.getCopyOfContextMap()

        return Runnable {
            withLoggingContext(mdcCopy) {
                runnable.run()
            }
        }
    }
}