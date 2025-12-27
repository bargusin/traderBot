package ru.rapidcoder.trader.bot.command.order;

import ru.rapidcoder.trader.bot.Bot;
import ru.tinkoff.piapi.contract.v1.OrderDirection;

public class BuyMarketCommand extends AbstractOrderCommand {

    public BuyMarketCommand(Bot bot, String identifier, String description) {
        super(bot, OrderDirection.ORDER_DIRECTION_BUY, identifier, description);
    }
}
