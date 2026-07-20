package ru.rapidcoder.trader.bot.command.settings;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.command.AbstractCommand;
import ru.rapidcoder.trader.bot.handler.InputHandler;
import ru.rapidcoder.trader.core.service.TradingMode;
import ru.rapidcoder.trader.core.database.repository.UserRepository;
import ru.rapidcoder.trader.core.service.EncryptionService;

public abstract class ChangeTokenCommand extends AbstractCommand {

    private final UserRepository userRepository;

    private final EncryptionService encryptionService;

    private final TradingMode mode;

    private final String modeName;

    public ChangeTokenCommand(Bot bot, String identifier, String description, TradingMode mode, String modeName) {
        super(bot, identifier, description);
        this.mode = mode;
        this.modeName = modeName;
        userRepository = new UserRepository(bot.getDatabaseManager());
        encryptionService = new EncryptionService(bot.getEncryptedKey());
    }

    @Override
    public void execute(Update update) {
        Long chatId = getChatId(update);

        processMessage(update, "Введите новый " + modeName + " токен:", cancelKeyboard());

        userStateService.setInputHandler(chatId, new InputHandler() {
            @Override
            public boolean handleInput(Update update) {
                if (checkToken(update.getMessage()
                        .getText())) {
                    updateToken(mode, update);
                    processMessage(update, "\u2705 Новое значение для " + modeName + " токена успешно установлено", settingsKeyboard());
                    return true;
                } else {
                    processMessage(update, "\uD83D\uDEAB Не удалось установить значение " + modeName + " токена", settingsKeyboard());
                    return false;
                }
            }
        });
    }

    protected void updateToken(TradingMode mode, Update update) {
        userRepository.updateToken(getChatId(update), mode, encryptionService.encrypt(update.getMessage()
                .getText()));
    }

    abstract boolean checkToken(String token);

}
