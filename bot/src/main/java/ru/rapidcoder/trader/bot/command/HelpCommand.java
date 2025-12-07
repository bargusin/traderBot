package ru.rapidcoder.trader.bot.command;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.component.KeyboardButton;

import java.util.List;

public class HelpCommand extends AbstractCommand {

    public HelpCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
    }

    @Override
    public void execute(Update update) {
        String text = """
                \uD83D\uDCAC <b>Помощь по боту</b>
                
                <b>Основные команды:</b>
                /start - Главное меню
                /help - Помощь
                
                """;

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(List.of(List.of(new KeyboardButton("\uD83C\uDFE0 Главное меню", "back_to_main"))));

        processMessage(update, text, keyboard);
    }
}
