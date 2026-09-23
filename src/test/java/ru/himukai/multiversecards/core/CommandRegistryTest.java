package ru.himukai.multiversecards.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandRegistryTest {

    @Test
    void duplicateNameIsRejected() {
        CommandRegistry registry = new CommandRegistry();
        registry.register(new Dummy("a"));

        assertThrows(IllegalArgumentException.class, () -> registry.register(new Dummy("A")));
    }

    @Test
    void findIgnoresSlashAndCase() {
        CommandRegistry registry = new CommandRegistry().register(new Dummy("about"));

        assertTrue(registry.find("/About").isPresent());
    }

    private record Dummy(String name) implements Command {
        @Override
        public String description() {
            return "";
        }

        @Override
        public String execute(CommandContext ctx) {
            return "";
        }
    }
}
