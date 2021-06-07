package io.github.ufukhalis.pretry.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.ufukhalis.pretry.logging.LoggerDelegate
import io.github.ufukhalis.pretry.model.EventHolder
import io.github.ufukhalis.pretry.model.RetryConfig
import org.mapdb.DB
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentMap
import javax.annotation.PreDestroy

const val LAST_DATE_KEY = "last_date_key"

@Service
class DbService(
    private val db: DB,
    private val configTable: ConcurrentMap<String, String>,
    private val eventHolderTable: ConcurrentMap<String, String>,
    private val eventRetryCountTable: ConcurrentMap<String, Int>,
    private val lastDateTable: ConcurrentMap<String, String>,
) {
    private val objectMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule())
    private val logger by LoggerDelegate()

    fun saveConfig(config: RetryConfig): Boolean {
        return configTable[config.identifier]?.let {
            false
        } ?: run {
            configTable[config.identifier] = objectMapper.writeValueAsString(config)
            true
        }
    }

    fun getConfig(identifier: String): RetryConfig? {
        return configTable[identifier]?.let {
            objectMapper.readValue<RetryConfig>(it)
        }
    }

    fun saveEvents(eventRetryDate: LocalDateTime, events: List<EventHolder>) {
        val newDate = eventRetryDate.withoutTime()
        eventHolderTable[newDate.toString()] = objectMapper.writeValueAsString(events)
    }

    fun getEvents(currentDate: LocalDateTime): List<EventHolder> {
        return eventHolderTable[currentDate.withoutTime().toString()]?.let {
            objectMapper.readValue<List<EventHolder>>(it)
        }?: listOf()
    }

    fun removeEvents(currentDate: LocalDateTime) {
        eventHolderTable.remove(currentDate.withoutTime().toString())
    }

    fun saveEventRetry(hash: String, currentRetryCount: Int) {
        eventRetryCountTable[hash] = currentRetryCount
    }

    fun getEventRetry(hash: String): Int? {
        return eventRetryCountTable[hash]
    }

    fun getLastDate(): LocalDateTime {
        return lastDateTable[LAST_DATE_KEY]?.let {
            LocalDateTime.parse(lastDateTable[LAST_DATE_KEY])
        } ?: run {
            LocalDateTime.now().withoutTime()
        }
    }

    fun saveLastDate(lastDate: LocalDateTime) {
        lastDateTable[LAST_DATE_KEY] = lastDate.withoutTime().toString()
    }

    @PreDestroy
    fun closeDb() {
        logger.info("Closing DB...")
        db.commit()
        db.close()
        logger.info("DB closed.")
    }
}

fun LocalDateTime.withoutTime(): LocalDateTime {
    return this.withSecond(0).withNano(0)
}
