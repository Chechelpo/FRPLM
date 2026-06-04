package chechelpo.frplm.domain.lorebook.entry.core;

import chechelpo.frplm.domain.lorebook.core.LorebookTestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Import(LorebookTestContext.class)
class EntryServiceTest {
    @Autowired LorebookTestContext lorebookTestContext;
}