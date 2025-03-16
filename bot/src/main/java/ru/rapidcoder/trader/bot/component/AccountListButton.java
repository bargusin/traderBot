package ru.rapidcoder.trader.bot.component;

public class AccountListButton extends MenuItemButton {

    private static String TEXT = "Список счетов";

    private static String CALLBACK_DATA = "getAccountList";

    public AccountListButton() {
        setText(TEXT);
        setCallbackData(CALLBACK_DATA);
    }

    @Override
    public String execute() {
        return "Список счетов...";
    }
}
