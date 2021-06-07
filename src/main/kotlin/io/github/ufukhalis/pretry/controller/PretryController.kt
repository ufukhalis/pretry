package io.github.ufukhalis.pretry.controller

import io.github.ufukhalis.pretry.model.CreateConfigRequest
import io.github.ufukhalis.pretry.model.ScheduleRetryRequest
import io.github.ufukhalis.pretry.service.PrettyService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class PretryController(
    val prettyService: PrettyService
) {

    @PostMapping("/v1/config")
    fun createConfig(@RequestBody request: CreateConfigRequest): ResponseEntity<String> {
        return prettyService.createConfig(request)
    }

    @PostMapping("/v1/event")
    fun scheduleRetry(@RequestBody request: ScheduleRetryRequest): ResponseEntity<String> {
        return prettyService.scheduleRetry(request)
    }
}
