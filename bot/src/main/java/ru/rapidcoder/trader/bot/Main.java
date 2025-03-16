package ru.rapidcoder.trader.bot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.rapidcoder.trader.core.ResourcesAdapter;

public class Main {

    public static final String BOT_NAME = ResourcesAdapter.getProperties("bot.properties").get("botName").toString();
    public static final String TOKEN_ID = ResourcesAdapter.getProperties("bot.properties").get("tokenId").toString();

    public static void main(String[] args) {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(new Bot(BOT_NAME, TOKEN_ID));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
