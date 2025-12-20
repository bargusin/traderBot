package ru.rapidcoder.trader.core.service;

import ru.tinkoff.piapi.contract.v1.Account;
import ru.tinkoff.piapi.contract.v1.MoneyValue;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AccountService {

    private final TradingSessionManager tradingSessionManager;

    private String accountId;

    public AccountService(TradingSessionManager tradingSessionManager) {
        this.tradingSessionManager = tradingSessionManager;
    }

    public static String formatMoney(MoneyValue money) {
        if (money == null)
            return "0.00";
        double value = money.getUnits() + money.getNano() / 1_000_000_000.0;
        return String.format("%.2f %s", value, money.getCurrency()
                .toUpperCase());
    }

    public static String formatMoney(Quotation quotation) {
        if (quotation == null)
            return "0.00";
        double value = quotation.getUnits() + quotation.getNano() / 1_000_000_000.0;
        return String.format("%.2f", value);
    }

    public CompletableFuture<List<Account>> getActiveAccounts(Long chatId) {
        InvestApi api = tradingSessionManager.getApi(chatId);
        TradingMode mode = tradingSessionManager.getCurrentMode(chatId);
        if (mode == TradingMode.SANDBOX) {
            var sandboxService = api.getSandboxService();
            return sandboxService.getAccounts()
                    .thenCompose(accounts -> {
                        if (!accounts.isEmpty()) {
                            return CompletableFuture.completedFuture(accounts);
                        }
                        return sandboxService.openAccount()
                                .thenCompose(newAccountId -> sandboxService.getAccounts());
                    });
        } else {
            return api.getUserService()
                    .getAccounts();
        }
    }
}
