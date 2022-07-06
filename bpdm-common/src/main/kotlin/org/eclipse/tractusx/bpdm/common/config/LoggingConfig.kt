package org.eclipse.tractusx.bpdm.common.config

import mu.withLoggingContext
import org.slf4j.MDC
import org.springframework.core.task.TaskDecorator
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.HtmlUtils
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class UserLoggingFilter(
    private val logConfigProperties: LogConfigProperties
) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val userName = request.userPrincipal?.name ?: logConfigProperties.unknownUser
        val escapedUserName = HtmlUtils.htmlEscape(userName)
        val escapedRequest = HtmlUtils.htmlEscape(request.requestURI)
        val escapedMethod = HtmlUtils.htmlEscape(request.method)

        withLoggingContext(
            "user" to escapedUserName,
        ) {
            logger.info("User '$escapedUserName' requests $escapedMethod $escapedRequest...")
            filterChain.doFilter(request, response)
            logger.info("Response with status ${response.status}")
        }
    }
}

@Component
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