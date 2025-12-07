package ru.rapidcoder.trader.bot.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.command.CommandRegistry;
import ru.rapidcoder.trader.bot.command.HelpCommand;
import ru.rapidcoder.trader.bot.command.StartCommand;

public class MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private final Bot bot;

    private final CommandRegistry commandRegistry;

    public MessageHandler(Bot bot) {
        this.bot = bot;
        this.commandRegistry = new CommandRegistry(bot);

        commandRegistry.registry(new StartCommand(bot, "/start", "Старт"));
        commandRegistry.registry(new HelpCommand(bot, "/help", "Помощь"));

        commandRegistry.registry(new StartCommand(bot, "back_to_main", "Возврат в основное меню"));
    }

    public void handleCommand(Update update) {
        Long userId = update.getMessage()
                .getFrom()
                .getId();

        if (!hasAccess(userId)) {
            logger.info("User dosn't access to bot by userId={}", userId);
            //TODO
        } else {
            commandRegistry.retrieveCommand(update.getMessage()
                            .getText())
                    .execute(update);
        }
    }

    public void handleCallback(Update update) {
        String callbackData = update.getCallbackQuery()
                .getData();
        String callbackId = update.getCallbackQuery()
                .getId();
        Long userId = update.getCallbackQuery()
                .getFrom()
                .getId();

        if (!hasAccess(userId)) {
            logger.info("User dosn't access to bot by userId={}", userId);
            bot.showNotification(callbackId, "Доступ к боту запрещен");
        } else {
            commandRegistry.retrieveCommand(callbackData)
                    .execute(update);
        }
    }

    private boolean hasAccess(Long userId) {
        return true;
    }
}
