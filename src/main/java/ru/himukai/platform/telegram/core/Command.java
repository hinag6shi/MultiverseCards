package ru.himukai.platform.telegram.core;

import java.util.List;

public interface Command {
    String name();

    String description();

    default String usage() {
        return "/" + name();
    }

    String execute(List<String> args);
}