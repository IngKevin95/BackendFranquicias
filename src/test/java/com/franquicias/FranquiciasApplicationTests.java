package com.franquicias;

import org.junit.jupiter.api.Test;

class FranquiciasApplicationTests {

    @Test
    void mainClassExists() {
        // Placeholder-free smoke check: class loads without throwing.
        assertDoesNotThrow(() -> Class.forName("com.franquicias.FranquiciasApplication"));
    }

    private static void assertDoesNotThrow(org.junit.jupiter.api.function.Executable e) {
        try {
            e.execute();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }
}
