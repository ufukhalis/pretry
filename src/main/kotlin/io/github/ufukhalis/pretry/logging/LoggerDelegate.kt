package io.github.ufukhalis.pretry.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class LoggerDelegate<in R : Any> : ReadOnlyProperty<R, Logger> {
    override fun getValue(thisRef: R, property: KProperty<*>): Logger = getLogger(thisRef.javaClass)

    companion object {
        val logger: Logger by LoggerDelegate()
    }
}
