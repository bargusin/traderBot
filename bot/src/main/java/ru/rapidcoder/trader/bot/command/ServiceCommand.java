package ru.rapidcoder.trader.bot.command;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.rapidcoder.trader.bot.handler.KeyboardManager;

abstract class ServiceCommand extends BotCommand {

    private static final Logger logger = LoggerFactory.getLogger(ServiceCommand.class);

    protected KeyboardManager keyboardManager = KeyboardManager.getKeyboardManager();

    public ServiceCommand(String identifier, String description) {
        super(identifier, description);
    }

    /**
     * Отправка ответа пользователю
     */
    void sendAnswer(AbsSender absSender, Long chatId, String text, ReplyKeyboard replyKeyboard) {
        if (StringUtils.isEmpty(text)) {
            text = "Message is empty";
        }
        SendMessage message = new SendMessage();
        message.enableMarkdown(true);
        message.setParseMode(ParseMode.HTML);
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(replyKeyboard);

        sendAnswer(absSender, message);
    }

    void sendAnswer(AbsSender absSender, SendMessage message) {
        try {
            absSender.execute(message);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

}
