package org.acme.fancy.lib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Use e.g. <a href="https://testcontainers.com/">testcontainers</a> for integration tests...
 */
class JavaLibraryTemplateIntegrationTest {

    @Test
    void toFunkyCase() {
        // given
        String input = "hello world!!!";

        // when
        String output = JavaLibraryTemplate.toFunkyCase(input);

        // then
        assertEquals("hElLo wOrLd!!!", output);
    }
}
