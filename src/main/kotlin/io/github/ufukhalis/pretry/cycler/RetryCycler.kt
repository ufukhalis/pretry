package io.github.ufukhalis.pretry.cycler

import io.github.ufukhalis.pretry.logging.LoggerDelegate
import io.github.ufukhalis.pretry.model.HttpIntegration
import io.github.ufukhalis.pretry.model.SqsIntegration
import io.github.ufukhalis.pretry.service.DbService
import io.github.ufukhalis.pretry.service.PrettyService
import io.github.ufukhalis.pretry.service.withoutTime
import kotlinx.coroutines.*
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime


private const val CYCLE_RATE = 60000L

@Component
@EnableScheduling
class RetryCycler(
    val dbService: DbService,
    val prettyService: PrettyService
) {

    private val logger by LoggerDelegate()

    @Scheduled(fixedRate = CYCLE_RATE, initialDelayString = "\${pretry.initial-delay}")
    fun startCycler() {
        val currentDate = LocalDateTime.now().withoutTime()
        var lastDate = dbService.getLastDate()

        logger.info("Current date is $currentDate and last date is $lastDate")

        while (lastDate.isBefore(currentDate) || currentDate.isEqual(lastDate)) {

            logger.info("Retry cycler has started for the date $lastDate...")

            runBlocking {
                val jobs : List<Job> = dbService.getEvents(lastDate).map { eventHolder ->

                    launch(context = Dispatchers.IO) {
                        logger.info("Event will be processed for this identifier ${eventHolder.identifier}")

                        dbService.getConfig(eventHolder.identifier)?.let { config ->
                            config.toIntegrations().map { integration ->
                                when(integration) {
                                    is HttpIntegration -> HttpRetryer(integration, prettyService)
                                    is SqsIntegration -> SqsRetryer(integration, prettyService)
                                }
                            }.forEach {
                                it.apply(eventHolder.eventBody, eventHolder.identifier)
                            }
                        }
                    }
                }
                jobs.joinAll()

                logger.info("Retry cycler completed!")
                dbService.removeEvents(lastDate)

                lastDate = lastDate.plusMinutes(1)

                dbService.saveLastDate(lastDate)
            }
        }
    }
}
