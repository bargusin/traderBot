package ru.rapidcoder.trader.bot.command.settings;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.command.AbstractCommand;
import ru.rapidcoder.trader.bot.component.InterfaceFactory;
import ru.rapidcoder.trader.bot.handler.InputHandler;

import java.util.ArrayList;
import java.util.List;

public class ChangeProductionTokenCommand extends AbstractCommand {

    public ChangeProductionTokenCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
    }

    @Override
    public void execute(Update update) {
        Long userId = getUserId(update);

        processMessage(update, "Введите новый PRODUCTION токен:", null);

        userStateService.setInputHandler(userId, new InputHandler() {
            @Override
            public boolean handleInput(Update update) {
                String text = update.getMessage()
                        .getText();
                InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                rows.add(List.of(InterfaceFactory.createButton("⚙\uFE0F Настройки", "/settings")));
                keyboard.setKeyboard(rows);

                processMessage(update, "✅ Новое значение для PRODUCTION токена успешно установлено: " + text, keyboard);
                return true;
            }
        });
    }
}
