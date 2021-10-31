package io.github.ufukhalis.pretry.cycler

import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.ufukhalis.pretry.logging.LoggerDelegate
import io.github.ufukhalis.pretry.model.Integration
import io.github.ufukhalis.pretry.model.SqsIntegration
import io.github.ufukhalis.pretry.service.PrettyService
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

class SqsRetryer(
    private val integration: Integration,
    private val prettyService: PrettyService
): Retryer {

    private val logger by LoggerDelegate()

    override fun apply(eventBody: ObjectNode, identifier: String) {
        val sqsIntegration = integration as SqsIntegration

        logger.info("Retrying with $sqsIntegration for identifier $identifier event $eventBody")

        val sqsClient = buildSqsClient(sqsIntegration)

        val messageRequest = SendMessageRequest.builder()
            .queueUrl(sqsIntegration.url)
            .messageBody(eventBody.asText())
            .build()

        runCatching {
            sqsClient.sendMessage(messageRequest)
        }.onFailure {
            logger.error("Event could not be pushed to SQS for this identifier $identifier")
            logger.warn("Event will be scheduled again if it's not reached its max retry count $identifier")

            scheduleRetry(eventBody, identifier, prettyService)
        }.onSuccess {
            logger.info("Event successfully pushed to SQS for this identifier $identifier")
        }

    }

    private fun buildSqsClient(sqsIntegration: SqsIntegration) =
        SqsClient.builder()
            .region(Region.of(sqsIntegration.region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    object : AwsCredentials {
                        override fun accessKeyId(): String {
                            return sqsIntegration.accessKey
                        }

                        override fun secretAccessKey(): String {
                            return sqsIntegration.secretKey
                        }
                    }
                )
            ).build()
}
