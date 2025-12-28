package ru.rapidcoder.trader.bot.command.account;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.command.AbstractCommand;
import ru.rapidcoder.trader.bot.component.InterfaceFactory;
import ru.rapidcoder.trader.bot.component.KeyboardButton;
import ru.rapidcoder.trader.core.service.TradingMode;
import ru.tinkoff.piapi.contract.v1.Account;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.SandboxService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AccountCommand extends AbstractCommand {

    public AccountCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
    }

    @Override
    public void execute(Update update) {
        String text = InterfaceFactory.format(
                bot.getTradingSessionManager().getCurrentMode(getChatId(update)),
                "\uD83D\uDCB0 <b>Выбор счета</b>"
        );

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        getActiveAccounts(getChatId(update)).handle((accounts, ex) -> {
            if (ex == null && accounts != null) {
                for (Account account : accounts) {
                    rows.add(List.of(createAccountButton(account)));
                }
            }
            rows.add(List.of(InterfaceFactory.createButton("\uD83C\uDFE0 Главное меню", "back_to_main")));
            keyboard.setKeyboard(rows);

            String finalText = (ex == null) ? text : text + "\n\n\uD83D\uDEAB Не удалось получить список счетов: " + ex.getMessage();
            processMessage(update, finalText, keyboard);
            return null;
        });
    }

    public CompletableFuture<List<Account>> getActiveAccounts(Long chatId) {
        InvestApi api = bot.getTradingSessionManager().getApi(chatId);
        TradingMode mode = bot.getTradingSessionManager().getCurrentMode(chatId);
        if (mode == TradingMode.SANDBOX) {
            SandboxService sandboxService = api.getSandboxService();
            return sandboxService.getAccounts().thenCompose(accounts -> {
                if (!accounts.isEmpty()) {
                    return CompletableFuture.completedFuture(accounts);
                }
                return sandboxService.openAccount().thenCompose(newAccountId -> sandboxService.getAccounts());
            });
        } else {
            return api.getUserService().getAccounts();
        }
    }

    private KeyboardButton createAccountButton(Account account) {
        return InterfaceFactory.createButton(
                (account.getName().isEmpty() ? "Брокерский счет" : account.getName()) + " (ID: " + account.getId() + ")",
                "switch_account#" + account.getId()
        );
    }
}
