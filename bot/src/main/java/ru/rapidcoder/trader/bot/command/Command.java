package ru.rapidcoder.trader.bot.command;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface Command {

    String getIdentifier();

    void execute(Update update);

}
