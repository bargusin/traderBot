package ru.rapidcoder.trader.bot;

import ru.rapidcoder.trader.core.TradingMode;

public class BotContext {

    private static BotContext instance;
    private TradingMode currentMode;

    private BotContext() {
        this.currentMode = TradingMode.SANDBOX;
    }

    public static synchronized BotContext getInstance() {
        if (instance == null) {
            instance = new BotContext();
        }
        return instance;
    }

    public TradingMode getMode() {
        return currentMode;
    }

    public void setMode(TradingMode mode) {
        this.currentMode = mode;
    }

    public boolean isTradingAllowed() {
        return currentMode.isTradingAllowed();
    }
}
