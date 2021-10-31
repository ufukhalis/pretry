package io.github.ufukhalis.pretry.cycler

import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.ufukhalis.pretry.model.ScheduleRetryRequest
import io.github.ufukhalis.pretry.service.PrettyService
import org.springframework.http.ResponseEntity

interface Retryer {

    fun apply(eventBody: ObjectNode, identifier: String)

    fun scheduleRetry(eventBody: ObjectNode, identifier: String, prettyService: PrettyService): ResponseEntity<String> {
        val scheduleRetryRequest = ScheduleRetryRequest(
            identifier, eventBody
        )
        return prettyService.scheduleRetry(scheduleRetryRequest)
    }
}
