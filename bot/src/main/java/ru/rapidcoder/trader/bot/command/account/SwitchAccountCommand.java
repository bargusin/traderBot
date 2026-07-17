package ru.rapidcoder.trader.bot.command.account;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.command.AbstractCommand;

public class SwitchAccountCommand extends AbstractCommand {

    public SwitchAccountCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
    }

    @Override
    public void execute(Update update) {
        String text = buildHeaderText(update, "\uD83D\uDCB0 <b>Счет выбран</b>");

        String accountId = getSuffix(update.getCallbackQuery()
                .getData());

        bot.getTradingSessionManager()
                .switchAccountId(getChatId(update), accountId);

        text += "\n\nСчет для торговли: <b>" + accountId + "</b>\n";

        processMessage(update, text, backToMainKeyboard());
    }
}
