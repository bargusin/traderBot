package ru.rapidcoder.trader.bot.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.service.UserStateService;

public abstract class AbstractCommand implements Command {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCommand.class);

    protected final Bot bot;
    protected final UserStateService userStateService = UserStateService.getInstance();
    private final String identifier;
    private final String description;

    public AbstractCommand(Bot bot, String identifier, String description) {
        this.bot = bot;
        this.identifier = identifier;
        this.description = description;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getDescription() {
        return description;
    }

    protected String getSuffix(String input) {
        if (input == null) {
            return null;
        }
        int index = input.indexOf('#');
        if (index == -1 || index == input.length() - 1) {
            return null;
        }
        return input.substring(index + 1);
    }

    protected Long getChatId(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery()
                    .getMessage()
                    .getChatId();
        } else if (update.hasMessage()) {
            return update.getMessage()
                    .getChatId();
        }
        throw new IllegalStateException("Не удалось получить chatId из Update");
    }

    protected Integer getMessageId(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery()
                    .getMessage()
                    .getMessageId();
        } else if (update.hasMessage()) {
            // Для обычных сообщений messageId часто не нужен для редактирования,
            // но может пригодиться для reply. Возвращаем null или ID сообщения.
            return null;
        }
        return null;
    }

    protected Long getUserId(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery()
                    .getFrom()
                    .getId();
        } else if (update.hasMessage()) {
            return update.getMessage()
                    .getFrom()
                    .getId();
        }
        throw new IllegalStateException("Не удалось получить userId из Update");
    }

    protected String getUserName(Update update) {
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery()
                    .getFrom()
                    .getUserName();
        } else if (update.hasMessage()) {
            return update.getMessage()
                    .getFrom()
                    .getUserName();
        }
        throw new IllegalStateException("Не удалось получить userName из Update");
    }

    protected void processMessage(Update update, String text, InlineKeyboardMarkup keyboard) {
        if (getMessageId(update) != null) {
            updateMessage(getChatId(update), getMessageId(update), text, keyboard);
        } else {
            sendMessage(getChatId(update), text, keyboard);
        }
    }

    private void sendMessage(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode(ParseMode.HTML);
        message.setReplyMarkup(keyboard);
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void updateMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(text);
        message.setMessageId(messageId);
        message.setParseMode(ParseMode.HTML);
        message.setReplyMarkup(keyboard);
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
