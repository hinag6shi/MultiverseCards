package ru.himukai.platform.telegram.commands;

public class AboutCommand {
    public SendMessage execute(Update update) {
        String chatId = update.getMessage().getChatId().toString();

        String about = "**Multiverse Cards** — это коллекционная игра,\n" +
                "где игрокам предстоит соревноваться с помощью карточек\n" +
                "персонажей и существ из разных вселенных.\n\n" +
                "Вы развиваете свою коллекцию, получаете стартовый набор ресурсов,\n" +
                "усиливаете карточки и взаимодействуете с другими игроками! \n" +
                "Покорите все вселенные!\n\n" +
                "Желаем Вам удачи в развитии!";

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode("Markdown"); // Включает поддержку жирного шрифта и списков

        return message;
    }
}
