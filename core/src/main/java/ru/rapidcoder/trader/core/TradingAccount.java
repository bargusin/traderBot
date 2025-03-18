package ru.rapidcoder.trader.core;

import ru.tinkoff.piapi.contract.v1.Account;

import java.util.List;

public class TradingAccount extends TradingObject {

    public List<Account> getAccountList() {
        return getTradingService().grtInvestApi()
                .getUserService()
                .getAccountsSync();
    }

}
