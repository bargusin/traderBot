package ru.rapidcoder.trader.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradingService {

    private static final Logger logger = LoggerFactory.getLogger(TradingService.class);

    private static TradingService instance;

    private TradingService() {

    }

    public static TradingService getTradingService() {
        if (instance == null) {
            instance = new TradingService();
        }
        return instance;
    }
}
