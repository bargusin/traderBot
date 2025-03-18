package ru.rapidcoder.trader.bot.component;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.rapidcoder.trader.core.TradingService;

public abstract class MenuItemButton extends InlineKeyboardButton implements Component {
    protected final TradingService tradingService = TradingService.getTradingService();

}
