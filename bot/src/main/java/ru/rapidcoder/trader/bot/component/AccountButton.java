package ru.rapidcoder.trader.bot.component;

import ru.tinkoff.piapi.contract.v1.Account;
import ru.tinkoff.piapi.core.models.Portfolio;

import java.util.List;

public class AccountButton extends MenuItemButton {

    private static final String TEXT = "Список счетов";
    private static final String CALLBACK_DATA = "getAccountList";

    public AccountButton() {
        setText(TEXT);
        setCallbackData(CALLBACK_DATA);
    }

    @Override
    public String execute() {
        StringBuilder builder = new StringBuilder();
        List<Account> accounts = tradingService.grtInvestApi()
                .getUserService()
                .getAccountsSync();
        for (Account account : accounts) {
            Portfolio portfolio = tradingService.grtInvestApi()
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
