package io.github.ufukhalis.pretry

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import java.io.File

@SpringBootTest
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProjectPretryApplicationTests {

    init {
        File("pretry_file.db").delete()
    }

    @Test
    fun contextLoads() {
    }

}
