package ru.himukai.multiversecards.commands;

import ru.himukai.multiversecards.core.Button;
import ru.himukai.multiversecards.core.Command;
import ru.himukai.multiversecards.core.CommandContext;
import ru.himukai.multiversecards.core.CommandRegistry;
import ru.himukai.multiversecards.core.Keyboard;
import ru.himukai.multiversecards.core.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * /help — список команд с кнопками, /help &lt;команда&gt; — справка по одной команде.
 * Кнопки строятся из реестра, поэтому новые команды появляются в списке сами.
 */
public final class HelpCommand implements Command {

    private static final int BUTTONS_PER_ROW = 2;

    private final CommandRegistry registry;

    public HelpCommand(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String description() {
        return "Список команд или справка по одной команде";
    }

    @Override
    public String usage() {
        return "/help [команда]";
    }

    @Override
    public Response execute(CommandContext ctx) {
        if (ctx.args().isEmpty()) {
            return overview();
        }
        String requested = ctx.args().getFirst();
        return registry.find(requested)
                .map(this::details)
                .orElseGet(() -> Response.text("Команда «" + requested + "» не найдена. Список команд: /" + name()));
    }

    private Response overview() {
        StringBuilder text = new StringBuilder("Доступные команды:\n");
        List<Button> buttons = new ArrayList<>();
        for (Command command : registry.all()) {
            text.append('/').append(command.name()).append(" — ").append(command.description()).append('\n');
            buttons.add(Button.command("/" + command.name(), name(), command.name()));
        }
        text.append("\nПодробнее: /help <команда>");
        return Response.update(text.toString(), Keyboard.grid(buttons, BUTTONS_PER_ROW));
    }

    private Response details(Command command) {
        Button back = Button.command("← Все команды", name());
        return Response.update(command.usage() + "\n" + command.description(), Keyboard.grid(List.of(back), 1));
    }
}
