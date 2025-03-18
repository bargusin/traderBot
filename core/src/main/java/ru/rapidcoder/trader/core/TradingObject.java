package ru.rapidcoder.trader.core;

public class TradingObject {

    protected TradingService tradingService = TradingService.getTradingService();

    public TradingService getTradingService() {
        return tradingService;
    }
}
