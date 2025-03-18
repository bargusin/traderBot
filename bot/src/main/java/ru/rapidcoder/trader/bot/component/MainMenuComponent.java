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

    public void addMenuButton(MenuItemButton button) {
        handlerExcecutor.add(new Handler(button));
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        rowInline.add(button);
        rowsInline.add(rowInline);
    }
    public void addBackButton(MenuItemButton button) {
        handlerExcecutor.add(new Handler(button));
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        rowInline.add(button);
        rowsInline.add(rowInline);
    }

    public InlineKeyboardMarkup getKeyboardMarkup() {
        return keyboardMarkup;
    }
}
