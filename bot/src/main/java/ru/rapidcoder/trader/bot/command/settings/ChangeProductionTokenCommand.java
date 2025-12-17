package ru.rapidcoder.trader.bot.command.settings;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.component.InterfaceFactory;
import ru.rapidcoder.trader.bot.handler.InputHandler;
import ru.rapidcoder.trader.core.service.TradingMode;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.ArrayList;
import java.util.List;

public class ChangeProductionTokenCommand extends ChangeTokenCommand {

    public ChangeProductionTokenCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
    }

    @Override
    public void execute(Update update) {
        Long chatId = getChatId(update);

        processMessage(update, "Введите новый PRODUCTION токен:", null);

        userStateService.setInputHandler(chatId, new InputHandler() {
            @Override
            public boolean handleInput(Update update) {
                InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                rows.add(List.of(InterfaceFactory.createButton("⚙\uFE0F Настройки", "/settings")));
                keyboard.setKeyboard(rows);

                if (checkToken(update.getMessage()
                        .getText())) {
                    updateToken(TradingMode.PRODUCTION, update);
                    processMessage(update, "✅ Новое значение для PRODUCTION токена успешно установлено", keyboard);
                    return true;
                } else {
                    processMessage(update, "\uD83D\uDEAB Не удалось установить значение PRODUCTION токена", keyboard);
                    return false;
                }
            }
        });
    }

    protected boolean checkToken(String token) {
        try {
            InvestApi api = InvestApi.create(token);
            api.getUserService()
                    .getAccountsSync();
            return true;
        } catch (Exception e) {
            logger.error("Ошибка установки нового PRODUCTION токена", e);
            return false;
        }
    }
}
