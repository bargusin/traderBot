package ru.rapidcoder.trader.bot.command;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.component.InterfaceFactory;
import ru.rapidcoder.trader.core.database.entity.User;
import ru.rapidcoder.trader.core.database.repository.UserRepository;
import ru.rapidcoder.trader.core.service.TradingMode;

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
            user.setCurrentMode(TradingMode.SANDBOX);
            userRepository.save(user);
        }

        String text = buildHeaderText(update, "\uD83C\uDFE0 <b>Главное меню</b>");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(InterfaceFactory.createButton(BotConstants.BUTTON_PORTFOLIO, BotConstants.CMD_PORTFOLIO)));
        rows.add(List.of(
                InterfaceFactory.createButton("\uD83D\uDED2 Купить", BotConstants.CMD_PURCHASE),
                InterfaceFactory.createButton("\uD83D\uDED2 Продать", BotConstants.CMD_SELL)
        ));
        rows.add(List.of(InterfaceFactory.createButton("\uD83D\uDCCB Ордера", BotConstants.CMD_ORDERS)));
        rows.add(List.of(InterfaceFactory.createButton(BotConstants.BUTTON_SETTINGS, BotConstants.CALLBACK_SETTINGS), InterfaceFactory.createButton(BotConstants.BUTTON_HELP, BotConstants.CMD_HELP)));
        keyboard.setKeyboard(rows);

        processMessage(update, text, keyboard);
    }
}
