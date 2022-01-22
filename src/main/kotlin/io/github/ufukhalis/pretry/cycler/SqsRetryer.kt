package io.github.ufukhalis.pretry.cycler

import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.ufukhalis.pretry.logging.LoggerDelegate
import io.github.ufukhalis.pretry.model.Integration
import io.github.ufukhalis.pretry.model.SqsIntegration
import io.github.ufukhalis.pretry.service.PrettyService
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import java.net.URI

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
            logger.error("Event could not be pushed to SQS for this identifier $identifier cause $it")
            logger.warn("Event will be scheduled again if it's not reached its max retry count for this identifier = $identifier")

            scheduleRetry(eventBody, identifier, prettyService)
        }.onSuccess {
            logger.info("Event successfully pushed to SQS for this identifier $identifier")
        }

    }

    private fun buildSqsClient(sqsIntegration: SqsIntegration): SqsClient {
        if (sqsIntegration.accessKey == "test") {
            return prepareLocalClient(sqsIntegration)
        }
        return SqsClient.builder()
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


    private fun prepareLocalClient(sqsIntegration: SqsIntegration) = SqsClient.builder()
        .region(Region.of(sqsIntegration.region))
        .endpointOverride(URI.create(sqsIntegration.url.substring(0, sqsIntegration.url.indexOf("/1"))))
        .credentialsProvider(AnonymousCredentialsProvider.create())
        .build()
}
