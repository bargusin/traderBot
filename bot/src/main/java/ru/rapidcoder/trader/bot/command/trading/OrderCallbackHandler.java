package ru.rapidcoder.trader.bot.command.trading;

import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.command.AbstractCommand;
import ru.rapidcoder.trader.bot.command.BotConstants;
import ru.rapidcoder.trader.core.service.MarketDataService;
import ru.rapidcoder.trader.core.service.TradingService;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.UUID;

public class OrderCallbackHandler extends AbstractCommand {

    public OrderCallbackHandler(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
    }

    @Override
    public void execute(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = getChatId(update);

        InvestApi investApi = bot.getTradingSessionManager().getApi(chatId);
        String accountId = bot.getTradingSessionManager().getCurrentAccountId(chatId);

        if (StringUtils.isEmpty(accountId)) {
            processMessage(update, "\uD83D\uDEAB Счет не определен. Используйте /account", backToMainKeyboard());
            return;
        }

        String suffix = getSuffix(callbackData);
        if (suffix == null) {
            processMessage(update, "\uD83D\uDEAB Некорректные данные callback", backToMainKeyboard());
            return;
        }

        TradingService tradingService = new TradingService(investApi);
        String headerText = buildHeaderText(update, "\uD83D\uDED2 <b>Ордер</b>");

        if (callbackData.startsWith(BotConstants.CALLBACK_CONFIRM_ORDER)) {
            handleConfirmOrder(update, chatId, headerText, tradingService, investApi, accountId, suffix);
        } else if (callbackData.startsWith(BotConstants.CALLBACK_CANCEL_ORDER)) {
            handleCancelOrder(update, chatId, headerText, tradingService, accountId, suffix);
        }
    }

    private void handleConfirmOrder(Update update, Long chatId, String headerText,
                                    TradingService tradingService, InvestApi investApi, String accountId, String suffix) {
        // suffix format: BUY_AAPL_10 or SELL_AAPL_10
        String[] parts = suffix.split("_");
        if (parts.length < 3) {
            processMessage(update, headerText + "\n\n\uD83D\uDEAB Некорректные данные ордера", backToMainKeyboard());
            return;
        }

        String directionStr = parts[0];
        String ticker = parts[1];
        long lots;
        try {
            lots = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            processMessage(update, headerText + "\n\n\uD83D\uDEAB Некорректное количество лотов", backToMainKeyboard());
            return;
        }

        OrderDirection direction = "BUY".equals(directionStr)
                ? OrderDirection.ORDER_DIRECTION_BUY
                : OrderDirection.ORDER_DIRECTION_SELL;

        MarketDataService marketDataService = new MarketDataService(investApi);

        marketDataService.findInstrumentByTicker(ticker)
                .thenCompose(instrumentOpt -> {
                    if (instrumentOpt.isEmpty()) {
                        processMessage(update, headerText + "\n\n\uD83D\uDEAB Инструмент не найден: " + ticker, backToMainKeyboard());
                        return java.util.concurrent.CompletableFuture.completedFuture(null);
                    }
                    InstrumentShort instrument = instrumentOpt.get();
                    Quotation price = Quotation.getDefaultInstance();

                    return tradingService.placeOrder(accountId, instrument.getFigi(), lots, price, direction, OrderType.ORDER_TYPE_MARKET)
                            .thenApply(response -> {
                                String status = mapExecStatus(response.getExecutionReportStatus());
                                String text = headerText + "\n\n"
                                        + "\u2705 Ордер исполнен\n\n"
                                        + "\uD83D\uDCB1 <b>" + instrument.getName() + "</b> (" + instrument.getTicker() + ")\n"
                                        + "\uD83D\uDCB2 Направление: " + directionStr + "\n"
                                        + "\uD83D\uDCCA Лотов: " + response.getLotsExecuted() + " / " + response.getLotsRequested() + "\n"
                                        + "\uD83D\uDCCB Статус: " + status + "\n"
                                        + "\uD83D\uDCC3 Ордер: <code>" + response.getOrderId() + "</code>";

                                processMessage(update, text, backToMainKeyboard());
                                return response;
                            });
                })
                .exceptionally(ex -> {
                    logger.error("Ошибка подтверждения ордера для chatId={}: {}", chatId, ex.getMessage(), ex);
                    String errorMsg = TradingService.mapErrorToMessage(ex);
                    processMessage(update, headerText + "\n\n\uD83D\uDEAB " + errorMsg, backToMainKeyboard());
                    return null;
                });
    }

    private void handleCancelOrder(Update update, Long chatId, String headerText,
                                   TradingService tradingService, String accountId, String orderId) {
        tradingService.cancelOrder(accountId, orderId)
                .thenAccept(cancelTime -> {
                    String text = headerText + "\n\n"
                            + "\u2705 Ордер отменён\n"
                            + "\uD83D\uDCC3 Ордер: <code>" + orderId + "</code>";

                    processMessage(update, text, backToMainKeyboard());
                })
                .exceptionally(ex -> {
                    logger.error("Ошибка отмены ордера {} для chatId={}: {}", orderId, chatId, ex.getMessage(), ex);
                    String errorMsg = TradingService.mapErrorToMessage(ex);
                    processMessage(update, headerText + "\n\n\uD83D\uDEAB " + errorMsg, backToMainKeyboard());
                    return null;
                });
    }

    private String mapExecStatus(OrderExecutionReportStatus status) {
        if (status == null) return "Неизвестно";
        return switch (status) {
            case EXECUTION_REPORT_STATUS_NEW -> "\uD83D\uDD34 Новый";
            case EXECUTION_REPORT_STATUS_PARTIALLYFILL -> "\uD83D\uDFE1 Частично исполнен";
            case EXECUTION_REPORT_STATUS_FILL -> "\uD83D\uDFE2 Исполнен";
            case EXECUTION_REPORT_STATUS_CANCELLED -> "\u26AB Отменён";
            case EXECUTION_REPORT_STATUS_REJECTED -> "\u26D4 Отклонён";
            default -> status.name();
        };
    }
}
