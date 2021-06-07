package io.github.ufukhalis.pretry.service

import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.ufukhalis.pretry.model.CreateConfigRequest
import io.github.ufukhalis.pretry.model.EventHolder
import io.github.ufukhalis.pretry.model.ScheduleRetryRequest
import io.github.ufukhalis.pretry.utils.HashUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PrettyService(
    val dbService: DbService
) {

    fun createConfig(request: CreateConfigRequest): ResponseEntity<String> {
        if (!dbService.saveConfig(request.toRetryConfig())) {
            return ResponseEntity.badRequest().body("Configuration is already set for this identifier!")
        }
        return ResponseEntity.ok("Configurations are created!")
    }

    fun scheduleRetry(request: ScheduleRetryRequest): ResponseEntity<String> {
        val hash = HashUtils.sha256("${request.identifier}${request.eventBody}")

        val retryStatus = getRetryStatus(hash, request.identifier)

        return when (retryStatus) {
            is RetryRejected -> ResponseEntity.status(retryStatus.status).body(retryStatus.message)
            is RetryExceeded -> ResponseEntity.status(retryStatus.status).body(retryStatus.message)
            is RetryAccepted -> {
                acceptEventForRetry(request.identifier, retryStatus.hour, request.eventBody)
                ResponseEntity.status(retryStatus.status).body(retryStatus.message)
            }
        }
    }

    private fun acceptEventForRetry(identifier: String, hour: Int, eventBody: ObjectNode) {
        val nextDate = LocalDateTime.now().plusHours(hour.toLong())

        val events = dbService.getEvents(nextDate)

        if (events.isNotEmpty()) {
            dbService.saveEvents(nextDate, events.plus(EventHolder(identifier, eventBody)))
        } else {
            dbService.saveEvents(nextDate, listOf(EventHolder(identifier, eventBody)))
        }
    }

    private fun getRetryStatus(hash: String, identifier: String) : RetryStatus {
        return dbService.getConfig(identifier)?.let { config ->
            dbService.getEventRetry(hash)?.let { currentRetryCount ->
                if (currentRetryCount >= config.maxRetry) {
                    RetryExceeded(
                        message = "The event retry exceed due to reached max retry count!",
                        status = HttpStatus.PRECONDITION_FAILED
                    )
                } else {
                    val newRetryCount = currentRetryCount + 1
                    dbService.saveEventRetry(hash, newRetryCount)
                    RetryAccepted(
                        message = "The event scheduled for retry $newRetryCount",
                        hour = config.retryHours[newRetryCount - 1],
                        status = HttpStatus.OK
                    )
                }
            } ?: run {
                dbService.saveEventRetry(hash, 1)
                RetryAccepted(
                    message = "The event scheduled for retry first time!",
                    status = HttpStatus.OK,
                    hour = config.retryHours[0]
                )
            }


        }?: run {
            RetryRejected(
                message = "Configurations are not found for this identifier!",
                status = HttpStatus.NOT_FOUND
            )
        }
    }
}

sealed class RetryStatus(open val message: String, open val status: HttpStatus)

data class RetryExceeded(override val message: String, override val status: HttpStatus) : RetryStatus(message, status)
data class RetryRejected(override val message: String, override val status: HttpStatus) : RetryStatus(message, status)
data class RetryAccepted(override val message: String, val hour: Int, override val status: HttpStatus): RetryStatus(message, status)
