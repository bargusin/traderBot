package ru.rapidcoder.trader.bot.command.settings;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.BotContext;
import ru.rapidcoder.trader.bot.command.AbstractCommand;
import ru.rapidcoder.trader.bot.component.InterfaceFactory;
import ru.rapidcoder.trader.core.service.TradingMode;

import java.util.ArrayList;
import java.util.List;

public class SwitchTradingModeCommand extends AbstractCommand {

    public SwitchTradingModeCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
    }

    @Override
    public void execute(Update update) {
        TradingMode mode = TradingMode.valueOf(getSuffix(update.getCallbackQuery()
                .getData()));
        BotContext.getInstance()
                .setMode(mode);

        String text = InterfaceFactory.format("\uD83C\uDFE0 <b>Смена режима работы бота</b>");

        text += "\n\nРежим работы бота успешно изменен на <b>" + mode + "</b>\n";

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(InterfaceFactory.createButton("⚙\uFE0F Настройки", "/settings")));
        keyboard.setKeyboard(rows);

        processMessage(update, text, keyboard);
    }
}
