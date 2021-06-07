package io.github.ufukhalis.pretry.model

import com.fasterxml.jackson.databind.node.ObjectNode

data class RetryConfig(
    val identifier : String,
    val maxRetry: Int,
    val retryHours: List<Int>,
    val integrations: List<RetryIntegration>
) {
    fun toIntegrations(): List<Integration> {
        return this.integrations.map {
            when (it.type) {
                IntegrationType.HTTP -> HttpIntegration(
                    url = it.config.get("url").asText(),
                    username = it.config.get("username").asText(),
                    password = it.config.get("password").asText()
                )
                IntegrationType.SQS -> SqsIntegration(
                    url = it.config.get("url").asText(),
                    secretKey = it.config.get("secretKey").asText(),
                    accessKey = it.config.get("accessKey").asText(),
                    region = it.config.get("region").asText(),
                )
            }
        }
    }
}

data class RetryIntegration(
    val type: IntegrationType,
    val config: ObjectNode
)

sealed class Integration
data class HttpIntegration(val url: String, val username:String, val password: String): Integration()
data class SqsIntegration(val url: String, val secretKey: String, val accessKey: String, val region: String): Integration()

enum class IntegrationType {
    HTTP, SQS
}

data class EventHolder(
    val identifier: String,
    val eventBody: ObjectNode
)
