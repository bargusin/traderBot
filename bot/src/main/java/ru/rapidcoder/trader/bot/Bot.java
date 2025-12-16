package ru.rapidcoder.trader.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.rapidcoder.trader.bot.handler.MessageHandler;
import ru.rapidcoder.trader.core.TradingMode;
import ru.rapidcoder.trader.core.database.DatabaseManager;

public class Bot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(Bot.class);
    private final MessageHandler messageHandler;
    private final String botName;
    private final DatabaseManager databaseManager;
    private final String encryptedKey;

    public Bot(String botName, String tokenId, String encryptedKey, String storageFile) {
        super(tokenId);
        this.botName = botName;

        this.databaseManager = DatabaseManager.getInstance(storageFile);
        this.encryptedKey = encryptedKey;
        BotContext.getInstance()
                .setMode(TradingMode.SANDBOX);

        this.messageHandler = new MessageHandler(this);
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                Long userId = message.getFrom()
                        .getId();
                long chatId = message.getChatId();
                logger.debug("Обработка сообщения chatId={}, userId={}", chatId, userId);
                if (message.hasText()) {
                    handleCommand(update);
                }
            } else if (update.hasCallbackQuery()) {
                handleCallback(update);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public boolean hasAccess(Long userId) {
        return userId != 232393084;
    }

    public void handleCommand(Update update) {
        messageHandler.handleCommand(update);
    }

    public void handleCallback(Update update) {
        messageHandler.handleCallback(update);
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public String getEncryptedKey() {
        return encryptedKey;
    }

    public void sendMessage(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode(ParseMode.HTML);
        message.setReplyMarkup(keyboard);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void updateMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(text);
        message.setMessageId(messageId);
        message.setParseMode(ParseMode.HTML);
        message.setReplyMarkup(keyboard);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void showNotification(String callbackQueryId, String text) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQueryId);
        answer.setText(text);
        answer.setShowAlert(false);
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
