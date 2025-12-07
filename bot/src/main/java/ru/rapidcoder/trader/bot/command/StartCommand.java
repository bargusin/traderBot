package ru.rapidcoder.trader.bot.command;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.component.KeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class StartCommand extends AbstractCommand {

    public StartCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
    }

    @Override
    public void execute(Update update) {
        String text = "\uD83C\uDFE0 <b>Главное меню</b>";

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(new KeyboardButton("\uD83D\uDCAC Помощь", "/help")));
        keyboard.setKeyboard(rows);

        processMessage(update, text, keyboard);
    }
}
