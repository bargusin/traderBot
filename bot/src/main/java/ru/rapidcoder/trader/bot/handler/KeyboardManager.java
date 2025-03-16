package ru.rapidcoder.trader.bot.handler;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Менеджер работы со стэком клавиатур
 */
public class KeyboardManager {

    private static final KeyboardManager keyboardManager = new KeyboardManager();
    private final Map<Long, Stack<InlineKeyboardMarkup>> userKeyboards;

    private KeyboardManager() {
        userKeyboards = new HashMap<>();
    }

    public static KeyboardManager getKeyboardManager() {
        return keyboardManager;
    }

    /**
     * Сокхранение клавиатуры в стеке для чата пользователя
     *
     * @param chatId   идентификатор чата
     * @param keyboard клавиатура
     */
    public void save(Long chatId, InlineKeyboardMarkup keyboard) {
        userKeyboards.putIfAbsent(chatId, new Stack<>());
        userKeyboards.get(chatId).push(keyboard);
    }

    /**
     * Получение с последующим удалением из стека клавиатуры
     *
     * @param chatId идентификатор чата
     * @return клавиатура
     */
    public InlineKeyboardMarkup get(Long chatId) {
        if (userKeyboards.containsKey(chatId) && !userKeyboards.get(chatId).isEmpty()) {
            return userKeyboards.get(chatId).pop();
        }
        return null;
    }
}
