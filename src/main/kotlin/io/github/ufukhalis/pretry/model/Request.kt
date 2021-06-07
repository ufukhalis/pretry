package io.github.ufukhalis.pretry.model

import com.fasterxml.jackson.databind.node.ObjectNode

data class CreateConfigRequest(
    val identifier: String,
    val maxRetry: Int,
    val retryHours: List<Int>,
    val integrations: List<IntegrationRequest>
) {
    fun toRetryConfig(): RetryConfig = RetryConfig(
        identifier = this.identifier,
        maxRetry = this.maxRetry,
        retryHours = this.retryHours,
        integrations = toRetryIntegration(this.integrations)
    )

    private fun toRetryIntegration(integrations: List<IntegrationRequest>): List<RetryIntegration> {
        return integrations.map {
            RetryIntegration(type = it.type, config = it.config)
        }
    }
}

data class ScheduleRetryRequest(
    val identifier: String,
    val eventBody: ObjectNode
)

data class IntegrationRequest(
    val type: IntegrationType,
    val config: ObjectNode
)
