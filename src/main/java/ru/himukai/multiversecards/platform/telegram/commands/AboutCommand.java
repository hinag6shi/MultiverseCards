package ru.himukai.multiversecards.platform.telegram.commands;

import ru.himukai.multiversecards.platform.telegram.core.Command;

import java.util.List;

public final class AboutCommand implements Command {

    @Override
    public String name() {
        return "about";
    }

    @Override
    public String description() {
        return "информация о назначении бота";
    }

    @Override
    public String execute(List<String> args) {
        return """
                Multiverse Cards — это коллекционная игра, \
                где игрокам предстоит соревноваться с помощью карточек \
                персонажей и существ из разных вселенных.
                
                Вы развиваете свою коллекцию, получаете стартовый набор ресурсов, \
                усиливаете карточки и взаимодействуете с другими игроками!
                Покорите все вселенные!
                
                Желаем вам удачи в развитии!""";
    }
}