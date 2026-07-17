package ru.rapidcoder.trader.bot.command.settings;

import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.core.service.TradingMode;
import ru.tinkoff.piapi.core.InvestApi;

public class ChangeProductionTokenCommand extends ChangeTokenCommand {

    public ChangeProductionTokenCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description, TradingMode.PRODUCTION, "PRODUCTION");
    }

    @Override
    protected boolean checkToken(String token) {
        try {
            InvestApi api = InvestApi.create(token);
            api.getUserService()
                    .getAccountsSync();
            return true;
        } catch (Exception e) {
            logger.error("Ошибка установки нового PRODUCTION токена", e);
            return false;
        }
    }
}
