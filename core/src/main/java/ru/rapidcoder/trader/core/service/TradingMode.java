package ru.rapidcoder.trader.core.service;

public enum TradingMode {

    SANDBOX("\uD83D\uDEE1\uFE0F", true), READONLY("\uD83D\uDD12", false), PRODUCTION("\uD83D\uDE80", true);

    private final String prefix;
    private final boolean tradingAllowed;

    TradingMode(String prefix, boolean tradingAllowed) {
        this.prefix = prefix;
        this.tradingAllowed = tradingAllowed;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isTradingAllowed() {
        return tradingAllowed;
    }

    public String getStorageKey() {
        if (this == READONLY) {
            return PRODUCTION.name();
        }
        return this.name();
    }
}
