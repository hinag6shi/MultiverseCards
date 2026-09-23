package ru.himukai.multiversecards.core;

import java.util.List;

public record CommandContext(String userId, List<String> args) {}
