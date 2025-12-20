package ru.rapidcoder.trader.core.service;

import ru.tinkoff.piapi.core.InvestApi;

public class TradingUserSession {

    private final InvestApi api;
    private final TradingMode mode;
    private final String accountId;
    private final long lastAccessTime;

    public TradingUserSession(InvestApi api, TradingMode mode, String accountId) {
        this.api = api;
        this.mode = mode;
        this.accountId = accountId;
        this.lastAccessTime = System.currentTimeMillis();
    }

    public InvestApi getApi() {
        return api;
    }

    public TradingMode getMode() {
        return mode;
    }

    public String getAccountId() {
        return accountId;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }
}
