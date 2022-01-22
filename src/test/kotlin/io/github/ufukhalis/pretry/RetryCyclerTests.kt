package io.github.ufukhalis.pretry

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import io.findify.sqsmock.SQSService
import io.github.ufukhalis.pretry.TestUtils.objectMapper
import io.github.ufukhalis.pretry.model.CreateConfigRequest
import io.github.ufukhalis.pretry.model.EventHolder
import io.github.ufukhalis.pretry.model.IntegrationRequest
import io.github.ufukhalis.pretry.model.IntegrationType
import io.github.ufukhalis.pretry.service.DbService
import io.github.ufukhalis.pretry.utils.HashUtils
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.util.SocketUtils
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import java.net.URI
import java.time.LocalDateTime
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RetryCyclerTests @Autowired constructor(val dbService: DbService) {

    private val identifier = UUID.randomUUID().toString()
    private lateinit var sqsClient: SqsClient

    init {
        TestUtils.deleteDb()
    }

    @BeforeAll
    fun `setup data` () {
        val availableWireMockPort = SocketUtils.findAvailableTcpPort()
        setupWireMock(availableWireMockPort)
        val availableSqsPort = SocketUtils.findAvailableTcpPort()
        setupSqs(availableSqsPort)
        sqsClient = testSqsClient(availableSqsPort)
        setupDb(availableWireMockPort, availableSqsPort)
    }

    @Order(1)
    @Test
    fun `should retry the past events from db` () {
        Thread.sleep(10000L)
        verify(1, postRequestedFor(
            urlEqualTo("/url")
        ))
    }

    private fun setupWireMock(port: Int) {
        val wireMockServer = WireMockServer(port)
        wireMockServer.start()

        configureFor(port)
        stubFor(
            post(
                urlEqualTo("/url")
            ).willReturn(aResponse().withStatus(200))
        )
    }
    
    private fun setupSqs(port: Int) {
        val sqs = SQSService(port, 1)
        sqs.start()
    }

    private fun testSqsClient(port: Int): SqsClient {
        val client = SqsClient.builder()
            .region(Region.of("eu-central-1"))
            .endpointOverride(URI.create("http://localhost:$port"))
            .credentialsProvider(AnonymousCredentialsProvider.create())
            .build()

        client.createQueue(
            CreateQueueRequest.builder()
                .queueName("sqs")
                .build()
        )
        return client
    }

    private fun setupDb(httpPort: Int, sqsPort: Int) {
        val config = CreateConfigRequest(
            identifier = identifier,
            maxRetry = 1,
            retryHours = listOf(1),
            integrations = listOf(
                IntegrationRequest(
                    type = IntegrationType.HTTP,
                    config = objectMapper.createObjectNode()
                        .put("url", "http://localhost:$httpPort/url")
                        .put("username", "username")
                        .put("password", "password")
                ),
                IntegrationRequest(
                    type = IntegrationType.SQS,
                    config = objectMapper.createObjectNode()
                        .put("url", "http://localhost:$sqsPort/1/sqs")
                        .put("region", "eu-central-1")
                        .put("secretKey", "test")
                        .put("accessKey", "test")
                )
            )
        )

        dbService.saveConfig(config.toRetryConfig())

        val eventBody = objectMapper.createObjectNode()
            .put("username", "ufuk")
            .put("email", "ufukhalis@gmail.com")

        val hash = HashUtils.hash(identifier, eventBody)
        dbService.saveEventRetry(hash, 1)

        val executionDate = LocalDateTime.now()
        dbService.saveEvents(executionDate, listOf(EventHolder(identifier, eventBody)))
    }
}
