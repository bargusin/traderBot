package ru.rapidcoder.trader.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.rapidcoder.trader.bot.command.StartCommand;

public class Bot extends TelegramLongPollingCommandBot {

    private static final Logger logger = LoggerFactory.getLogger(Bot.class);
    private final String botName;

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

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public void processNonCommandUpdate(Update update) {
        if (update.hasMessage()) {
            // Обработка обычных сообщений
        } else if (update.hasCallbackQuery()) {
            Message msg = (Message) update.getCallbackQuery().getMessage();
            sendMessage(msg.getChatId(), "Test");
        }
    }
}
