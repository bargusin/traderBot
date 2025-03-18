package ru.rapidcoder.trader.bot;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.rapidcoder.trader.bot.command.StartCommand;
import ru.rapidcoder.trader.bot.handler.HandlerExcecutor;
import ru.rapidcoder.trader.bot.handler.KeyboardManager;

public class Bot extends TelegramLongPollingCommandBot {

    private static final Logger logger = LoggerFactory.getLogger(Bot.class);

    private final String botName;

    private final HandlerExcecutor handlerExcecutor = HandlerExcecutor.getHandlerExcecutor();

    protected KeyboardManager keyboardManager = KeyboardManager.getKeyboardManager();

    public Bot(String botName, String tokenId) {
        super(tokenId);
        this.botName = botName;

        register(new StartCommand("start", "Старт"));
    }

    /**
     * Отправка ответа
     *
     * @param chatId id чата
     * @param text   текст ответа
     */
    private void sendMessage(Long chatId, String text) {
        SendMessage answer = new SendMessage();
        answer.setText(text);
        answer.setChatId(chatId.toString());
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void sendEditMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboardMarkup) {
        if (!StringUtils.isEmpty(text)) {
            EditMessageText messageText = EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(text)
                    .build();
            try {
                execute(messageText);
            } catch (TelegramApiException e) {
                logger.error(e.getMessage(), e);
            }
        }

        EditMessageReplyMarkup messageReplyMarkup = EditMessageReplyMarkup.builder()
                .messageId(messageId)
                .chatId(chatId)
                .replyMarkup(keyboardMarkup)
                .build();
        // Необходимо поместить в стэк текущую клавиатуру
        keyboardManager.save(chatId, keyboardMarkup);
        try {
            execute(messageReplyMarkup);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public void processNonCommandUpdate(Update update) {
        if (update.hasMessage()) {
            // Обработка обычных сообщений
        } else if (update.hasCallbackQuery()) {
            Message msg = (Message) update.getCallbackQuery()
                    .getMessage();
            String result = handlerExcecutor.execute(update.getCallbackQuery()
                    .getData());
            sendEditMessage(msg.getChatId(), msg.getMessageId(), result, keyboardManager.get(msg.getChatId()));
        }
    }
}
