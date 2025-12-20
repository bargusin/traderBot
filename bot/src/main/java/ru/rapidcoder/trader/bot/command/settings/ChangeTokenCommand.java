package ru.rapidcoder.trader.bot.command.settings;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.command.AbstractCommand;
import ru.rapidcoder.trader.core.service.TradingMode;
import ru.rapidcoder.trader.core.database.repository.UserRepository;
import ru.rapidcoder.trader.core.service.EncryptionService;

public abstract class ChangeTokenCommand extends AbstractCommand {

    private final UserRepository userRepository;

    private final EncryptionService encryptionService;

    public ChangeTokenCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
        userRepository = new UserRepository(bot.getDatabaseManager());
        encryptionService = new EncryptionService(bot.getEncryptedKey());
    }

    protected void updateToken(TradingMode mode, Update update) {
        userRepository.updateToken(getChatId(update), mode, encryptionService.encrypt(update.getMessage()
                .getText()));
    }

    abstract boolean checkToken(String token);

}
