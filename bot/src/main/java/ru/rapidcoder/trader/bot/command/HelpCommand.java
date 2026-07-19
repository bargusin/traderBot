package ru.rapidcoder.trader.bot.command;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.rapidcoder.trader.bot.Bot;

import java.util.List;

public class HelpCommand extends AbstractCommand {

    public HelpCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
    }

    @Override
    public void execute(Update update) {
        String text = """
                \uD83D\uDCAC <b>Помощь по боту</b>

                <b>Основные команды:</b>
                /start — Главное меню
                /help — Помощь (этот раздел)
                /cancel — Отмена текущего ввода

                <b>Счета и портфель:</b>
                /account — Выбор и управление брокерскими счетами
                /portfolio — Просмотр портфеля: стоимость, позиции, доходность

                <b>Настройки:</b>
                /settings — Настройки бота: режим работы (SANDBOX/READONLY/PRODUCTION), API-токены, счета

                <b>Рыночные данные:</b>
                /price &lt;ticker&gt; — Текущая цена и статус торгов инструмента
                  Пример: /price SBER, /price BBG0013HGFT4
                  Принимает: тикер, FIGI или ISIN
                /chart &lt;ticker&gt; — Свечной график инструмента за 30 дней
                  Пример: /chart AAPL
                  Доступны периоды: 1ч, 4ч, 1д, 1н (через inline-кнопки)
                """;

        processMessage(update, text, backToMainKeyboard());
    }
}
