package io.github.ufukhalis.pretry.controller

import io.github.ufukhalis.pretry.TestUtils.deleteDb
import io.github.ufukhalis.pretry.TestUtils.objectMapper
import io.github.ufukhalis.pretry.model.CreateConfigRequest
import io.github.ufukhalis.pretry.model.ScheduleRetryRequest
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PretryControllerTests @Autowired constructor(val mockMvc: MockMvc) {

    private val identifier = "controller-identifier"

    init {
        deleteDb()
    }

    @Order(1)
    @Test
    fun `should create config for unique identifier` () {
        val request = CreateConfigRequest(
            identifier = UUID.randomUUID().toString(),
            maxRetry = 3,
            retryHours = listOf(1, 2, 3),
            integrations = listOf()
        )

        mockMvc.post("/v1/config") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { is2xxSuccessful() }
        }
    }

    @Order(2)
    @Test
    fun `should return 400 when creating config for same identifier` () {
        val request = CreateConfigRequest(
            identifier = identifier,
            maxRetry = 1,
            retryHours = listOf(1),
            integrations = listOf()
        )

        mockMvc.post("/v1/config") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { is2xxSuccessful() }
        }

        mockMvc.post("/v1/config") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { is4xxClientError() }
        }
    }

    @Order(3)
    @Test
    fun `should schedule retry if not exceeded max retry` () {
        val request = ScheduleRetryRequest(
            identifier = identifier,
            eventBody = objectMapper.createObjectNode().put("test", "test")
        )

        mockMvc.post("/v1/event") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { is2xxSuccessful() }
        }
    }

    @Order(4)
    @Test
    fun `should return 412 if max retry exceeded` () {
        val request = ScheduleRetryRequest(
            identifier = identifier,
            eventBody = objectMapper.createObjectNode().put("test", "test")
        )

        mockMvc.post("/v1/event") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { is4xxClientError() }
        }
    }

    @Order(5)
    @Test
    fun `should return 404 if no configuration found for that identifier` () {
        val request = ScheduleRetryRequest(
            identifier = UUID.randomUUID().toString(),
            eventBody = objectMapper.createObjectNode().put("test", "test")
        )

        mockMvc.post("/v1/event") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { is4xxClientError() }
        }
    }
}
