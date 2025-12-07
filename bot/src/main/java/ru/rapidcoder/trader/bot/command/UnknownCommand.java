package ru.rapidcoder.trader.bot.command;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.rapidcoder.trader.bot.Bot;

public class UnknownCommand extends AbstractCommand {

    public UnknownCommand(Bot bot) {
        this(bot, "unknownCommand", "Unknown command");
    }

    private UnknownCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
    }

    @Override
    public void execute(Update update) {
        processMessage(update, getIdentifier(), null);
    }
}
