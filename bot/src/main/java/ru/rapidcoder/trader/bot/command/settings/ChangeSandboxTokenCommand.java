package ru.rapidcoder.trader.bot.command.settings;

import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.core.service.TradingMode;
import ru.tinkoff.piapi.core.InvestApi;

public class ChangeSandboxTokenCommand extends ChangeTokenCommand {

    public ChangeSandboxTokenCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description, TradingMode.SANDBOX, "SANDBOX");
    }

    @Override
    protected boolean checkToken(String token) {
        try {
            InvestApi sandboxApi = InvestApi.createSandbox(token);
            sandboxApi.getUserService()
                    .getAccountsSync();
            return true;
        } catch (Exception e) {
            logger.error("Ошибка установки нового SANDBOX токена", e);
            return false;
        }
    }
}
