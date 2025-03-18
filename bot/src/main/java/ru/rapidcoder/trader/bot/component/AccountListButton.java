package ru.rapidcoder.trader.bot.component;

import ru.rapidcoder.trader.core.TradingAccount;
import ru.tinkoff.piapi.contract.v1.Account;
import ru.tinkoff.piapi.core.models.Portfolio;

import java.util.List;

public class AccountListButton extends MenuItemButton {

    private static final String TEXT = "Список счетов";
    private static final String CALLBACK_DATA = "getAccountList";
    private final TradingAccount tradingAccount = new TradingAccount();

    public AccountListButton() {
        setText(TEXT);
        setCallbackData(CALLBACK_DATA);
    }

    @Override
    public String execute() {
        StringBuilder builder = new StringBuilder();
        List<Account> accounts = tradingAccount.getAccountList();
        for (Account account : accounts) {
            Portfolio portfolio = tradingAccount.getTradingService()
                    .grtInvestApi()
                    .getOperationsService()
                    .getPortfolioSync(account.getId());
            builder.append(String.format("Production account: %s, %s (%s) \n", account.getName(), portfolio.getTotalAmountShares()
                    .getValue()
                    .doubleValue(), portfolio.getTotalAmountShares()
                    .getCurrency()));
        }
        return builder.toString();
    }

    public String getCallbackData() {
        return CALLBACK_DATA;
    }
}
