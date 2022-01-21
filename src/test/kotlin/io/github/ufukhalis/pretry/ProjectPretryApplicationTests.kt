package io.github.ufukhalis.pretry

import io.github.ufukhalis.pretry.TestUtils.deleteDb
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration


@SpringBootTest
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProjectPretryApplicationTests {

    init {
        deleteDb()
    }

    @Test
    fun contextLoads() {
    }

}
