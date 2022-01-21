package io.github.ufukhalis.pretry

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

object TestUtils {

    val objectMapper = ObjectMapper()
    const val identifier = "identifier"

    fun deleteDb() {
        File("pretry_file.db").delete()
    }
}
