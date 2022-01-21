package io.github.ufukhalis.pretry.config

import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.Serializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ConcurrentMap

@Configuration
class DbConfig (@Value("\${db.file.path}") val path: String){

    @Bean
    fun createDb(): DB {
        return DBMaker
            .fileDB(path)
            .fileMmapEnable()
            .make()
    }

    @Bean("configTable")
    fun createConfigTable(db: DB):ConcurrentMap<String, String> {
        return db.hashMap<String, String>("config_table", Serializer.STRING, Serializer.STRING)
            .createOrOpen()
    }

    @Bean("eventRetryCountTable")
    fun createEventRetryCountTable(db: DB):ConcurrentMap<String, Int> {
        return db.hashMap<String, Int>("event_retry_count_table", Serializer.STRING, Serializer.INTEGER)
            .createOrOpen()
    }

    @Bean("eventHolderTable")
    fun createEventHolderTable(db: DB):ConcurrentMap<String, String> {
        return db.hashMap<String, String>("event_holder_table", Serializer.STRING, Serializer.STRING)
            .createOrOpen()
    }

    @Bean("lastDateTable")
    fun createLastDateTable(db: DB):ConcurrentMap<String, String> {
        return db.hashMap<String, String>("last_date_table", Serializer.STRING, Serializer.STRING)
            .createOrOpen()
    }

}
