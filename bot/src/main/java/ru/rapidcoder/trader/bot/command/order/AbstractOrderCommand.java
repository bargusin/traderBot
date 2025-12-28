package ru.rapidcoder.trader.bot.command.order;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.command.AbstractCommand;
import ru.tinkoff.piapi.contract.v1.*;
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
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        String[] args = text.split(" ");

        // Простейшая валидация: /cmd TICKER LOTS
        if (args.length < 3) {
            processMessage(update, "⚠️ <b>Неверный формат!</b>\nИспользуйте: " + getIdentifier() + " [ТИКЕР] [ЛОТЫ]\nПример: `" + getIdentifier() + " SBER 1`", null);
            return;
        }

        String ticker = args[1].toUpperCase();
        long lots;
        try {
            lots = Long.parseLong(args[2]);
            if (lots <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            processMessage(update, "⚠️ Количество лотов должно быть целым положительным числом", null);
            return;
        }

        String accountId = bot.getTradingSessionManager().getCurrentAccountId(getChatId(update));
        if (accountId == null) {
            processMessage(update, "⚠️ Счет не выбран. Зайдите в настройки", null);
            return;
        }

        InvestApi api = bot.getTradingSessionManager().getApi(chatId);

        // Цепочка выполнения: Найти инструмент -> Выставить заявку
        processMessage(update, "⏳ Ищу " + ticker + " и выставляю заявку...", null);

        api.getInstrumentsService().findInstrument(ticker).thenCompose(instruments -> {
                    if (instruments.isEmpty()) {
                        throw new RuntimeException("Инструмент с тикером " + ticker + " не найден");
                    }
                    InstrumentShort instrument = null;
                    for (InstrumentShort item : instruments) {
                        if (item.getApiTradeAvailableFlag()) {
                            if ("TQBR".equals(item.getClassCode())) {
                                instrument = item;
                                break;
                            }
                            // Если это не TQBR, запоминаем как запасной вариант (на случай фондов или валюты)
                            if (instrument == null) {
                                instrument = item;
                            }
                        }
                    }

                    if (instrument == null) {
                        processMessage(update, "\uD83D\uDEAB Инструмент " + ticker + " найден, но недоступен для торговли через API.", null);
                    }

                    assert instrument != null;
                    String figi = instrument.getFigi();
                    String name = instrument.getName();

                    // Отправка рыночной заявки
                    return api.getOrdersService()
                            .postOrder(
                                    figi,
                                    lots,
                                    Quotation.getDefaultInstance(), // Цена для рыночной заявки не нужна
                                    orderDirection,
                                    accountId,
                                    OrderType.ORDER_TYPE_MARKET,
                                    UUID.randomUUID().toString()
                            ).thenApply(response -> new OrderResult(response, name));
                })
                .thenAccept(result -> {
                    String emoji = orderDirection == OrderDirection.ORDER_DIRECTION_BUY ? "🟢" : "🔴";
                    String action = orderDirection == OrderDirection.ORDER_DIRECTION_BUY ? "Покупка" : "Продажа";

                    String msg = String.format("%s <b>Успешная заявка!</b>\n\n" + "📄 Инструмент: <b>%s</b>\n" + "⚖️ Тип: <b>Рыночная %s</b>\n" + "📦 Лотов: <b>%d</b>\n" + "💵 Сумма сделки: <b>%s</b>\n" + "🔖 Статус: `%s`", emoji, result.instrumentName, action, result.response.getLotsRequested(), formatMoney(Money.fromResponse(result.response.getTotalOrderAmount())), result.response.getExecutionReportStatus()
                            .name());

                    processMessage(update, msg, null);
                })
                .exceptionally(ex -> {
                    processMessage(update, "\uD83D\uDEAB <b>Ошибка заявки:</b>\n" + ex.getMessage(), null);
                    return null;
                });
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
