package ru.rapidcoder.trader.bot.component;

import ru.rapidcoder.trader.core.service.TradingMode;

public class InterfaceFactory {

    public static String format(TradingMode mode, String text) {
        return text + " " + mode.getPrefix();
    }

    public static KeyboardButton createButton(String text, String callbackData) {
        return new KeyboardButton(text, callbackData);
    }
}
