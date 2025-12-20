package ru.rapidcoder.trader.bot.command;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.component.InterfaceFactory;
import ru.rapidcoder.trader.core.database.entity.User;
import ru.rapidcoder.trader.core.database.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class StartCommand extends AbstractCommand {

    private final UserRepository userRepository;

    public StartCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);

        userRepository = new UserRepository(bot.getDatabaseManager());
    }

    @Override
    public void execute(Update update) {
        if (!userRepository.existsByChatId(getChatId(update))) {
            logger.info("Создается новый пользователь для первого запуска бота");
            User user = new User();
            user.setChatId(getChatId(update));
            user.setUserName(getUserName(update));
            userRepository.save(user);
        }
        String text = InterfaceFactory.format(bot.getTradingSessionManager()
                .getCurrentMode(getChatId(update)), "\uD83C\uDFE0 <b>Главное меню</b>");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(InterfaceFactory.createButton("\uD83D\uDCBC Портфель", "/portfolio")));
        rows.add(List.of(InterfaceFactory.createButton("⚙\uFE0F Настройки", "/settings"), InterfaceFactory.createButton("\uD83D\uDCAC Помощь", "/help")));
        keyboard.setKeyboard(rows);

        processMessage(update, text, keyboard);
    }
}
