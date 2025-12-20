package ru.rapidcoder.trader.bot.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.command.CommandRegistry;
import ru.rapidcoder.trader.bot.command.HelpCommand;
import ru.rapidcoder.trader.bot.command.PortfolioCommand;
import ru.rapidcoder.trader.bot.command.StartCommand;
import ru.rapidcoder.trader.bot.command.account.AccountCommand;
import ru.rapidcoder.trader.bot.command.account.SwitchAccountCommand;
import ru.rapidcoder.trader.bot.command.settings.ChangeProductionTokenCommand;
import ru.rapidcoder.trader.bot.command.settings.ChangeSandboxTokenCommand;
import ru.rapidcoder.trader.bot.command.settings.SettingsCommand;
import ru.rapidcoder.trader.bot.command.settings.SwitchTradingModeCommand;
import ru.rapidcoder.trader.bot.service.UserStateService;

public class MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private final Bot bot;

    private final CommandRegistry commandRegistry;

    private final UserStateService userStateService = UserStateService.getInstance();

    public MessageHandler(Bot bot) {
        this.bot = bot;
        this.commandRegistry = new CommandRegistry(bot);

        commandRegistry.registry(new StartCommand(bot, "/start", "Старт"));
        commandRegistry.registry(new StartCommand(bot, "back_to_main", "Возврат в основное меню"));
        commandRegistry.registry(new HelpCommand(bot, "/help", "Помощь"));

        commandRegistry.registry(new AccountCommand(bot, "/account", "Управление счетами"));
        commandRegistry.registry(new SwitchAccountCommand(bot, "switch_account", "Выбор счета"));

        commandRegistry.registry(new PortfolioCommand(bot, "/portfolio", "Портфель"));

        commandRegistry.registry(new SettingsCommand(bot, "/settings", "Настройки"));
        commandRegistry.registry(new SwitchTradingModeCommand(bot, SettingsCommand.CallbackType.SWITCH_TRADING_MODE.getPrefix(), "Смена режима работы бота"));
        commandRegistry.registry(new ChangeSandboxTokenCommand(bot, "change_sandbox_token", "Установка SANDBOX токена"));
        commandRegistry.registry(new ChangeProductionTokenCommand(bot, "change_production_token", "Установка PRODUCTION токена"));
    }

    public void handleCommand(Update update) {
        String messageText = update.getMessage()
                .getText();
        Long userId = update.getMessage()
                .getFrom()
                .getId();

        if (bot.hasAccess(userId)) {
            bot.sendMessage(userId, "Доступ к боту запрещен", null);
        } else {
            if ("/cancel".equals(messageText)) {
                userStateService.clearState(userId);
                return;
            }

            InputHandler activeHandler = userStateService.getInputHandler(userId);
            if (activeHandler != null) {
                boolean finished = activeHandler.handleInput(update);
                if (finished) {
                    userStateService.clearState(userId);
                }
                return;
            }

            commandRegistry.retrieveCommand(update.getMessage()
                            .getText())
                    .execute(update);
        }
    }

    public void handleCallback(Update update) {
        String callbackData = update.getCallbackQuery()
                .getData();
        Long userId = update.getCallbackQuery()
                .getFrom()
                .getId();

        if (bot.hasAccess(userId)) {
            bot.sendMessage(userId, "Доступ к боту запрещен", null);
        } else {
            int index = callbackData.indexOf('#');
            if (index > 0) {
                callbackData = callbackData.substring(0, index);
            }
            commandRegistry.retrieveCommand(callbackData)
                    .execute(update);
        }
    }
}