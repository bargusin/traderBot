package ru.rapidcoder.trader.bot.component;

public class BackButton extends MenuItemButton {

    public BackButton(String text, String callbackData) {
        setText(text);
        setCallbackData(callbackData);
    }

    @Override
    public String execute() {
        return null;
    }
}
