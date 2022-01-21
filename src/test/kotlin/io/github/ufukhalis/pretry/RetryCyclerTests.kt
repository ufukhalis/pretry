package io.github.ufukhalis.pretry

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import io.github.ufukhalis.pretry.TestUtils.identifier
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
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RetryCyclerTests @Autowired constructor(val dbService: DbService) {

    init {
        TestUtils.deleteDb()
    }

    @BeforeAll
    fun `setup data` () {
        val availableWireMockPort = SocketUtils.findAvailableTcpPort()
        setupWireMock(availableWireMockPort)
        setupDb(availableWireMockPort)
    }

    @Order(1)
    @Test
    fun `should retry the past events from db` () {
        Thread.sleep(5000L)
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

    private fun setupDb(port: Int) {
        val config = CreateConfigRequest(
            identifier = identifier,
            maxRetry = 1,
            retryHours = listOf(1),
            integrations = listOf(
                IntegrationRequest(
                    type = IntegrationType.HTTP,
                    config = objectMapper.createObjectNode()
                        .put("url", "http://localhost:$port/url")
                        .put("username", "username")
                        .put("password", "password")
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
