package ru.rapidcoder.trader.bot.command;

import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.rapidcoder.trader.bot.component.AccountListButton;
import ru.rapidcoder.trader.bot.component.MainMenuComponent;

public class StartCommand extends ServiceCommand {

    private ReplyKeyboardMarkup replyKeyboardMarkup;

    public StartCommand(String identifier, String description) {
        super(identifier, description);
    }

    private InlineKeyboardMarkup createKeyboard() {
        MainMenuComponent menu = new MainMenuComponent();
        menu.addItem(new AccountListButton());

        return menu.getKeyboardMarkup();
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        InlineKeyboardMarkup keyboardMarkup = createKeyboard();
        keyboardManager.save(chat.getId(), keyboardMarkup);
        sendAnswer(absSender, chat.getId(), "Управление брокерским счетом:", keyboardMarkup);
    }

}
