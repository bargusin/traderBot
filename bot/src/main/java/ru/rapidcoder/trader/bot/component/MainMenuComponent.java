package ru.rapidcoder.trader.bot.component;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.rapidcoder.trader.bot.handler.Handler;
import ru.rapidcoder.trader.bot.handler.HandlerExcecutor;

import java.util.ArrayList;
import java.util.List;

public class MainMenuComponent {

    private final HandlerExcecutor handlerExcecutor = HandlerExcecutor.getHandlerExcecutor();

    private final InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();

    private final List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

    public MainMenuComponent() {
        keyboardMarkup.setKeyboard(rowsInline);
    }

    public void addItem(MenuItemButton item) {
        handlerExcecutor.add(new Handler(item));
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        rowInline.add(item);
        rowsInline.add(rowInline);
    }

    public InlineKeyboardMarkup getKeyboardMarkup() {
        return keyboardMarkup;
    }
}
