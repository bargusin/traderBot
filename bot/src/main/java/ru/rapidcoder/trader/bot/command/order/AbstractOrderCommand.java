package ru.rapidcoder.trader.bot.command.order;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.command.AbstractCommand;
import ru.rapidcoder.trader.bot.service.ApiCallExecutor;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.OrderType;
import ru.tinkoff.piapi.contract.v1.PostOrderResponse;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.models.Money;

import java.util.UUID;

public abstract class AbstractOrderCommand extends AbstractCommand {

    private final OrderDirection orderDirection;

    public AbstractOrderCommand(Bot bot, OrderDirection orderDirection, String identifier, String description) {
        super(bot, identifier, description);
        this.orderDirection = orderDirection;
    }

    @Override
    public void execute(Update update) {
        Long chatId = update.getMessage()
                .getChatId();
        String text = update.getMessage()
                .getText();
        String[] args = text.split(" ");

        // 1. Простейшая валидация: /cmd TICKER LOTS
        if (args.length < 3) {
            processMessage(update, "⚠️ **Неверный формат!**\nИспользуйте: " + getIdentifier() + " [ТИКЕР] [ЛОТЫ]\nПример: `" + getIdentifier() + " SBER 1`", null);
            return;
        }

        String ticker = args[1].toUpperCase();
        long lots;
        try {
            lots = Long.parseLong(args[2]);
            if (lots <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            processMessage(update, "⚠️ Количество лотов должно быть целым положительным числом.", null);
            return;
        }

        // 2. Получаем API и AccountID
        // (Используем ваш механизм получения accountId, который мы обсуждали ранее)
        String accountId = bot.getTradingSessionManager()
                .getCurrentAccountId(getChatId(update));
        if (accountId == null) {
            processMessage(update, "⚠️ Счет не выбран. Зайдите в настройки.", null);
            return;
        }

        InvestApi api = bot.getTradingSessionManager()
                .getApi(chatId);
        ApiCallExecutor executor = bot.getApiCallExecutor();

        // 3. Цепочка выполнения: Найти инструмент -> Выставить заявку
        processMessage(update, "⏳ Ищу " + ticker + " и выставляю заявку...", null);

        executor.execute(chatId, () ->
                // Шаг А: Ищем инструмент по тикеру
                api.getInstrumentsService()
                        .findInstrument(ticker)
                        .thenCompose(instruments -> {
                            // Проверяем список
                            if (instruments.isEmpty()) {
                                throw new RuntimeException("Инструмент с тикером " + ticker + " не найден.");
                            }

                            // Берем первый результат (обычно он самый релевантный)
                            // Обратите внимание: используем get(0), так как это Список
                            var instrument = instruments.get(0);

                            String figi = instrument.getFigi();
                            String name = instrument.getName();

                            // 2. Отправляем рыночную заявку
                            return api.getOrdersService()
                                    .postOrder(figi, lots, null, // Цена для рыночной заявки не нужна
                                            orderDirection, accountId, OrderType.ORDER_TYPE_MARKET, UUID.randomUUID()
                                                    .toString())
                                    .thenApply(response -> new OrderResult(response, name));
                        })
                        .thenAccept(result -> {
                            // 4. Успех
                            String emoji = orderDirection == OrderDirection.ORDER_DIRECTION_BUY ? "🟢" : "🔴";
                            String action = orderDirection == OrderDirection.ORDER_DIRECTION_BUY ? "Покупка" : "Продажа";

                            String msg = String.format("%s **Успешная заявка!**\n\n" + "📄 Инструмент: **%s**\n" + "⚖️ Тип: **Рыночная %s**\n" + "📦 Лотов: **%d**\n" + "💵 Сумма сделки: **%s**\n" + "🔖 Статус: `%s`", emoji, result.instrumentName, action, result.response.getLotsRequested(), formatMoney(Money.fromResponse(result.response.getTotalOrderAmount())),
                                    result.response.getExecutionReportStatus()
                                            .name());

                            processMessage(update, msg, null);

                        })
                        .exceptionally(ex -> {
                            // 5. Ошибка
                            // Если это ошибка токена - она уже обработана Executor'ом (вернется null)
                            if (ex == null)
                                return null;

                            String errorMsg = ex.getCause() != null ? ex.getCause()
                                    .getMessage() : ex.getMessage();
                            processMessage(update, "\uD83D\uDEAB **Ошибка заявки:**\n" + errorMsg, null);
                            return null;
                        }));
    }

    private String formatMoney(Money money) {
        if (money == null)
            return "?";
        return String.format("%.2f %s", money.getValue(), money.getCurrency());
    }

    private static class OrderResult {
        final PostOrderResponse response;
        final String instrumentName;

        public OrderResult(PostOrderResponse response, String instrumentName) {
            this.response = response;
            this.instrumentName = instrumentName;
        }
    }
}
