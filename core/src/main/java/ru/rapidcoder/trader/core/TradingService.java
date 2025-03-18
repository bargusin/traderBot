package ru.rapidcoder.trader.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.core.InvestApi;

public class TradingService {

    public static final String SANDBOX_TOKEN = ResourcesAdapter.getProperties("trading.properties")
            .get("sandboxToken")
            .toString();
    public static final String PRODUCTION_TOKEN = ResourcesAdapter.getProperties("trading.properties")
            .get("productionToken")
            .toString();

    private final Type type = Type.valueOf(ResourcesAdapter.getProperties("trading.properties")
            .get("tradingType")
            .toString());
    private static final Logger logger = LoggerFactory.getLogger(TradingService.class);
    private static final TradingService tradingService = new TradingService();

    private TradingService() {

    }

    public static TradingService getTradingService() {
        return tradingService;
    }

    public InvestApi grtInvestApi() {
        if (type.equals(Type.SANDBOX)) {
            return InvestApi.create(SANDBOX_TOKEN);
        } else {
            return InvestApi.create(PRODUCTION_TOKEN);
        }
    }

    public enum Type {
        SANDBOX, PRODUCTION
    }
}
