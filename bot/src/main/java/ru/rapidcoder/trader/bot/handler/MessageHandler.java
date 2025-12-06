package ru.rapidcoder.trader.bot.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.rapidcoder.trader.bot.Bot;

public class MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private final Bot bot;

    public MessageHandler(Bot bot) {
        this.bot = bot;
    }

    public void handleCommand(Update update) {
        Long chatId = update.getMessage()
                .getChatId();
        Long userId = update.getMessage()
                .getFrom()
                .getId();
        String messageText = update.getMessage()
                .getText();

        if (!hasAccess(userId)) {
            //TODO
        } else {
            if ("/start".equals(messageText)) {
                bot.showMainMenu(chatId, null);
            } else if ("/help".equals(messageText)) {
                bot.showHelpMenu(chatId, null);
            } else {

            }
        }
    }

    public void handleCallback(Update update) {
        String callbackData = update.getCallbackQuery()
                .getData();
        String callbackId = update.getCallbackQuery()
                .getId();
        Long chatId = update.getCallbackQuery()
                .getMessage()
                .getChatId();
        Long userId = update.getCallbackQuery()
                .getFrom()
                .getId();
        Integer messageId = update.getCallbackQuery()
                .getMessage()
                .getMessageId();

        if (!hasAccess(userId)) {
            logger.debug("User dosn't access to bot by userId={}", userId);
            bot.showNotification(callbackId, "Доступ к боту запрещен");
        } else {
            switch (callbackData) {
                case "menu_help" -> {
                    bot.showHelpMenu(chatId, messageId);
                }
                case "back_to_main" -> {
                    bot.showMainMenu(chatId, messageId);
                }
            }
        }
    }

    private boolean hasAccess(Long userId) {
        return true;
    }
}
