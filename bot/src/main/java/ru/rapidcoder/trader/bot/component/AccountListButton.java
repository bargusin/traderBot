package ru.rapidcoder.trader.bot.component;

public class AccountListButton extends MenuItemButton {

    private static final String TEXT = "Список счетов";

    private static final String CALLBACK_DATA = "getAccountList";

    public AccountListButton() {
        setText(TEXT);
        setCallbackData(CALLBACK_DATA);
    }

    @Override
    public String execute() {
        return "Параметры счета....";
    }

    public  String getCallbackData() {
        return CALLBACK_DATA;
    }
}
