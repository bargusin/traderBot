package ru.rapidcoder.trader.bot.command.account;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.command.AbstractCommand;
import ru.rapidcoder.trader.bot.component.InterfaceFactory;

import java.util.ArrayList;
import java.util.List;

public class SwitchAccountCommand extends AbstractCommand {

    public SwitchAccountCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
    }

    @Override
    public void execute(Update update) {
        String text = InterfaceFactory.format(bot.getTradingSessionManager()
                .getCurrentMode(getChatId(update)), "\uD83D\uDCB0 <b>Счет выбран</b>");

        String accountId = getSuffix(update.getCallbackQuery()
                .getData());

        bot.getTradingSessionManager()
                .switchAccountId(getChatId(update), accountId);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(InterfaceFactory.createButton("\uD83C\uDFE0 Главное меню", "back_to_main")));
        keyboard.setKeyboard(rows);

        text += "\n\nСчет для торговли: <b>" + accountId + "</b>\n";

        processMessage(update, text, keyboard);
    }
}
