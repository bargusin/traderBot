package ru.rapidcoder.trader.bot.component;

import ru.rapidcoder.trader.bot.BotContext;

public class InterfaceFactory {

    public static String format(String text) {
        String prefix = BotContext.getInstance()
                .getMode()
                .getPrefix();
        return text + " " + prefix;
    }

    public static KeyboardButton createButton(String text, String callbackData) {
        return new KeyboardButton(format(text), callbackData);
    }
}
