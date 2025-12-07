package ru.rapidcoder.trader.bot.component;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

public class KeyboardButton extends InlineKeyboardButton {

    public KeyboardButton(String text, String callbackData) {
        setText(text);
        setCallbackData(callbackData);
    }

}
