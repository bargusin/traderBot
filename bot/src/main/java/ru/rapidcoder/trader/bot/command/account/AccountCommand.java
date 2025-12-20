package ru.rapidcoder.trader.bot.command.account;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.command.AbstractCommand;
import ru.rapidcoder.trader.bot.component.InterfaceFactory;
import ru.rapidcoder.trader.bot.component.KeyboardButton;
import ru.tinkoff.piapi.contract.v1.Account;

import java.util.ArrayList;
import java.util.List;

public class AccountCommand extends AbstractCommand {

    public AccountCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
    }

    @Override
    public void execute(Update update) {
        String text = InterfaceFactory.format(bot.getTradingSessionManager()
                .getCurrentMode(getChatId(update)), "\uD83D\uDCB0 <b>Выбор счета</b>");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        bot.getTradingSessionManager()
                .getAccountService()
                .getActiveAccounts(getChatId(update))
                .handle((accounts, ex) -> {
                    // 1. Логика наполнения (Success path)
                    if (ex == null && accounts != null) {
                        for (Account account : accounts) {
                            rows.add(List.of(createAccountButton(account)));
                        }
                    }
                    rows.add(List.of(InterfaceFactory.createButton("\uD83C\uDFE0 Главное меню", "back_to_main")));
                    keyboard.setKeyboard(rows);

                    // 3. Формирование текста (зависит от того, была ошибка или нет)
                    String finalText = (ex == null) ? text : text + "\n\n\uD83D\uDEAB Не удалось получить список счетов: " + ex.getMessage();

                    processMessage(update, finalText, keyboard);

                    return null;
                });
    }

    private KeyboardButton createAccountButton(Account account) {
        return InterfaceFactory.createButton((account.getName()
                .isEmpty() ? "Брокерский счет" : account.getName()) + " (ID: " + account.getId() + ")", "switch_account#" + account.getId());
    }
}
