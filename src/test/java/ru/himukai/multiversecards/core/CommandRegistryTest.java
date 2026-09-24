package ru.himukai.multiversecards.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
        public Response execute(CommandContext ctx) {
            return Response.text("");
        }
    }
}
