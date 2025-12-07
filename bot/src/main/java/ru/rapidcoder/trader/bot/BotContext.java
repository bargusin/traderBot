package ru.rapidcoder.trader.bot;

import ru.rapidcoder.trader.bot.component.BotMode;

public class BotContext {

    private static BotContext instance;
    private BotMode currentMode;

    private BotContext() {
        this.currentMode = BotMode.SANDBOX;
    }

    public static synchronized BotContext getInstance() {
        if (instance == null) {
            instance = new BotContext();
        }
        return instance;
    }

    public BotMode getMode() {
        return currentMode;
    }

    public void setMode(BotMode mode) {
        this.currentMode = mode;
    }

    public boolean isTradingAllowed() {
        return currentMode.isTradingAllowed();
    }
}
