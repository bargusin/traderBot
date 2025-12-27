package ru.rapidcoder.trader.bot.command.order;

import ru.rapidcoder.trader.bot.Bot;
import ru.tinkoff.piapi.contract.v1.OrderDirection;

public class SellMarketCommand extends AbstractOrderCommand {

    public SellMarketCommand(Bot bot, String identifier, String description) {
        super(bot, OrderDirection.ORDER_DIRECTION_SELL, identifier, description);
    }
}
