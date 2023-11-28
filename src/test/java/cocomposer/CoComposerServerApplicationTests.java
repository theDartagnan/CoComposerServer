package cocomposer;

import cocomposer.config.TestDatasetConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("mongo-test")
@Import(TestDatasetConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CoComposerServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
