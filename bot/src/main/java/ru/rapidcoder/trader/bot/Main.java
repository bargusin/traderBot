package ru.rapidcoder.trader.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            String environment = System.getenv("botEnv") != null ? System.getenv("botEnv") : "dev";
            String botName = System.getenv(environment + "BotName");
            String tokenId = System.getenv(environment + "TokenId");
            String encryptedKey = System.getenv(environment + "EncryptedKey");
            String storageFile = System.getenv(environment + "StorageFile");

            TelegramBotsApi telegramBotsApi = createTelegramBotsApi();
            Bot bot = createBot(botName, tokenId, encryptedKey, storageFile);
            telegramBotsApi.registerBot(bot);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static TelegramBotsApi createTelegramBotsApi() throws TelegramApiException {
        return new TelegramBotsApi(DefaultBotSession.class);
    }

    public static Bot createBot(String botName, String tokenId, String encryptedKey, String storageFile) {
        return new Bot(botName, tokenId, encryptedKey, storageFile);
    }
}
