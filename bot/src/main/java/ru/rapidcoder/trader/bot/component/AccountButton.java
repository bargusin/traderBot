package ru.rapidcoder.trader.bot.component;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.tinkoff.piapi.contract.v1.Account;
import ru.tinkoff.piapi.core.models.Portfolio;

import java.util.List;

public class AccountButton extends MenuItemButton {

    public AccountButton(String text, String callbackData) {
        super(text, callbackData);
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

    private InlineKeyboardMarkup createKeyboard() {
        MainMenuComponent menu = new MainMenuComponent();
        menu.addButton(new AccountButton("Список счетов", "getAccountList"));

        menu.addButton(new BackButton("Назад", "backToMainMenu"));

        return menu.getKeyboardMarkup();
    }
}
