package io.github.ufukhalis.pretry.cycler

import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.ufukhalis.pretry.model.ScheduleRetryRequest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

interface Retryer {

    val restTemplate: RestTemplate
        get() = RestTemplate()

    fun apply(eventBody: ObjectNode, identifier: String)

    fun scheduleRetry(eventBody: ObjectNode, identifier: String): ResponseEntity<String> {
        val scheduleRetryRequest = ScheduleRetryRequest(
            identifier, eventBody
        )
        val scheduleRequest = HttpEntity<ScheduleRetryRequest>(scheduleRetryRequest)
        return restTemplate.exchange("http://localhost:8080/v1/event", HttpMethod.POST, scheduleRequest, String::class.java)
    }
}
