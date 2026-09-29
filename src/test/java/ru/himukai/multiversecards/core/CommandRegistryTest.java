package ru.himukai.multiversecards.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandRegistryTest {

    @Test
    void findIgnoresCaseAndLeadingSlash() {
        StubCommand help = new StubCommand("help");
        CommandRegistry registry = new CommandRegistry().register(help);

        assertSame(help, registry.find("help").orElseThrow());
        assertSame(help, registry.find("HELP").orElseThrow());
        assertSame(help, registry.find("/help").orElseThrow());
        assertSame(help, registry.find("  /Help ").orElseThrow());
    }

    @Test
    void findUnknownReturnsEmpty() {
        CommandRegistry registry = new CommandRegistry().register(new StubCommand("help"));

        assertTrue(registry.find("nope").isEmpty());
    }

    @Test
    void registerRejectsDuplicates() {
        CommandRegistry registry = new CommandRegistry().register(new StubCommand("help"));

        assertThrows(IllegalArgumentException.class, () -> registry.register(new StubCommand("help")));
        assertThrows(IllegalArgumentException.class, () -> registry.register(new StubCommand("HELP")));
    }

    @Test
    void registerRejectsBlankName() {
        CommandRegistry registry = new CommandRegistry();

        assertThrows(IllegalArgumentException.class, () -> registry.register(new StubCommand(" ")));
        assertThrows(IllegalArgumentException.class, () -> registry.register(new StubCommand("/")));
    }

    @Test
    void allReturnsCommandsInAlphabeticalOrder() {
        CommandRegistry registry = new CommandRegistry()
                .register(new StubCommand("help"), new StubCommand("about"), new StubCommand("author"));

        List<String> names = registry.all().stream().map(Command::name).toList();

        assertEquals(List.of("about", "author", "help"), names);
    }

    @Test
    void allIsReadOnly() {
        CommandRegistry registry = new CommandRegistry().register(new StubCommand("help"));

        assertThrows(UnsupportedOperationException.class, () -> registry.all().clear());
    }
}
