package io.github.ufukhalis.pretry.cycler

import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.ufukhalis.pretry.logging.LoggerDelegate
import io.github.ufukhalis.pretry.model.HttpIntegration
import io.github.ufukhalis.pretry.model.Integration
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod


class HttpRetryer(
    private val integration: Integration
): Retryer {

    private val logger by LoggerDelegate()

    override fun apply(eventBody: ObjectNode, identifier: String) {
        val httpIntegration = integration as HttpIntegration

        logger.info("Retrying with $httpIntegration for identifier $identifier event $eventBody")

        val headers = HttpHeaders()
        headers.setBasicAuth(httpIntegration.username, httpIntegration.password)
        val request = HttpEntity<String>(eventBody.asText(), headers)

        val response = restTemplate.exchange(httpIntegration.url, HttpMethod.POST, request, String::class.java)
        if (response.statusCode.is2xxSuccessful) {
            logger.info("Event successfully pushed to the url for this identifier $identifier")
        } else {
            logger.error("Event could not be pushed to the url for this identifier $identifier")
            logger.warn("Event will be scheduled again if it's not reached its max retry count $identifier")
            scheduleRetry(eventBody, identifier)
        }
    }
}
