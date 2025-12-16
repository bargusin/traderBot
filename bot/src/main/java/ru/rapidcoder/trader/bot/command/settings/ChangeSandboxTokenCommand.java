package ru.rapidcoder.trader.bot.command.settings;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.component.InterfaceFactory;
import ru.rapidcoder.trader.bot.handler.InputHandler;
import ru.rapidcoder.trader.core.TradingMode;

import java.util.ArrayList;
import java.util.List;

public class ChangeSandboxTokenCommand extends ChangeTokenCommand {

    public ChangeSandboxTokenCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
    }

    @Override
    public void execute(Update update) {
        Long chatId = getChatId(update);

        processMessage(update, "Введите новый SANDBOX токен:", null);

        userStateService.setInputHandler(chatId, new InputHandler() {
            @Override
            public boolean handleInput(Update update) {
                InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                rows.add(List.of(InterfaceFactory.createButton("⚙\uFE0F Настройки", "/settings")));
                keyboard.setKeyboard(rows);

                updateToken(TradingMode.SANDBOX, update);

                processMessage(update, "✅ Новое значение для SANDBOX токена успешно установлено", keyboard);
                return true;
            }
        });
    }
}
