package ru.rapidcoder.trader.bot.command.settings;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.command.AbstractCommand;
import ru.rapidcoder.trader.bot.component.InterfaceFactory;
import ru.rapidcoder.trader.bot.component.KeyboardButton;
import ru.rapidcoder.trader.core.TradingMode;

import java.util.ArrayList;
import java.util.List;

public class SettingsCommand extends AbstractCommand {

    public SettingsCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
    }

    @Override
    public void execute(Update update) {
        String text = InterfaceFactory.format("⚙\uFE0F <b>Настройки</b>");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(createTradingModeButton(TradingMode.SANDBOX), createTradingModeButton(TradingMode.READONLY), createTradingModeButton(TradingMode.PRODUCTION)));
        rows.add(List.of(InterfaceFactory.createButton("✏\uFE0F SANDBOX_TOKEN", "change_sandbox_token"), InterfaceFactory.createButton("✏\uFE0F PRODUCTION_TOKEN", "change_production_token")));
        rows.add(List.of(InterfaceFactory.createButton("\uD83C\uDFE0 Главное меню", "back_to_main")));
        keyboard.setKeyboard(rows);

        processMessage(update, text, keyboard);
    }

    private KeyboardButton createTradingModeButton(TradingMode mode) {
        return InterfaceFactory.createButton(mode.getPrefix() + " " + mode.name(), CallbackType.SWITCH_TRADING_MODE.getPrefix() + "#" + mode.name());
    }

    public enum CallbackType {
        SWITCH_TRADING_MODE("switch_trading_mode_to");

        private final String prefix;

        CallbackType(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
    }
}

