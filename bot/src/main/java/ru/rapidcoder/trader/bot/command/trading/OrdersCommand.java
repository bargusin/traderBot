package ru.rapidcoder.trader.bot.command.trading;

import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.command.AbstractCommand;
import ru.rapidcoder.trader.bot.command.BotConstants;
import ru.rapidcoder.trader.bot.component.InterfaceFactory;
import ru.rapidcoder.trader.core.service.MarketDataService;
import ru.rapidcoder.trader.core.service.TradingService;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class OrdersCommand extends AbstractCommand {

    public OrdersCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
    }

    @Override
    public void execute(Update update) {
        Long chatId = getChatId(update);
        String callbackData = update.hasCallbackQuery() ? update.getCallbackQuery().getData() : "";

        InvestApi investApi = bot.getTradingSessionManager().getApi(chatId);
        String accountId = bot.getTradingSessionManager().getCurrentAccountId(chatId);

        if (StringUtils.isEmpty(accountId)) {
            processMessage(update, "\uD83D\uDEAB Счет не определен. Используйте /account", backToMainKeyboard());
            return;
        }

        TradingService tradingService = new TradingService(investApi);
        String headerText = buildHeaderText(update, "\uD83D\uDCCB <b>Ордера</b>");

        boolean showHistory = callbackData.startsWith(BotConstants.CALLBACK_ORDERS_HISTORY);

        if (showHistory) {
            showOrders(update, chatId, headerText, tradingService, investApi, accountId, true);
        } else {
            showOrders(update, chatId, headerText, tradingService, investApi, accountId, false);
        }
    }

    private void showOrders(Update update, Long chatId, String headerText,
                            TradingService tradingService, InvestApi investApi, String accountId, boolean history) {
        tradingService.getAllOrders(accountId)
                .thenAccept(orders -> {
                    List<OrderState> filtered;
                    if (history) {
                        filtered = orders.stream()
                                .filter(o -> o.getExecutionReportStatus() == OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_FILL
                                        || o.getExecutionReportStatus() == OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_REJECTED
                                        || o.getExecutionReportStatus() == OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_CANCELLED)
                                .toList();
                    } else {
                        filtered = orders.stream()
                                .filter(o -> o.getExecutionReportStatus() != OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_FILL
                                        && o.getExecutionReportStatus() != OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_REJECTED
                                        && o.getExecutionReportStatus() != OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_CANCELLED)
                                .toList();
                    }

                    if (filtered.isEmpty()) {
                        String emptyMsg = history ? "\n\n\uD83D\uDCED Исполненных ордеров нет" : "\n\n\uD83D\uDCED Активных ордеров нет";
                        InlineKeyboardMarkup keyboard = createViewToggleKeyboard(history);
                        processMessage(update, headerText + emptyMsg, keyboard);
                        return;
                    }

                    StringBuilder sb = new StringBuilder(headerText);
                    sb.append(history ? "\n\n\uD83D\uDCD6 <b>История ордеров:</b>\n\n" : "\n\n\uD83D\uDD34 <b>Активные ордера:</b>\n\n");

                    for (OrderState order : filtered) {
                        sb.append(formatOrder(order, investApi));
                        if (!history) {
                            sb.append("    ❌ <a href=\"javascript:void(0)\" data-callback=\"")
                                    .append(BotConstants.CALLBACK_CANCEL_ORDER)
                                    .append("#").append(order.getOrderId())
                                    .append("\">Отменить</a>\n");
                        }
                        sb.append("\n");
                    }

                    InlineKeyboardMarkup keyboard = createViewToggleKeyboard(history);
                    processMessage(update, sb.toString(), keyboard);
                })
                .exceptionally(ex -> {
                    logger.error("Ошибка OrdersCommand для chatId={}: {}", chatId, ex.getMessage(), ex);
                    processMessage(update, headerText + "\n\n\uD83D\uDEAB Ошибка: " + ex.getMessage(), backToMainKeyboard());
                    return null;
                });
    }

    private String formatOrder(OrderState order, InvestApi investApi) {
        String direction = order.getDirection() == OrderDirection.ORDER_DIRECTION_BUY ? "\uD83D\uDCC8 ПОКУПКА" : "\uD83D\uDCC9 ПРОДАЖА";
        String status = mapOrderStatus(order.getExecutionReportStatus());

        return "📋 Ордер: <code>" + order.getOrderId() + "</code>\n"
                + "    " + direction + " | " + status + "\n"
                + "    Лотов: " + order.getLotsRequested() + " / " + order.getLotsExecuted() + "\n\n";
    }

    private String mapOrderStatus(OrderExecutionReportStatus status) {
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

    private InlineKeyboardMarkup createViewToggleKeyboard(boolean currentlyHistory) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton>> rows = new ArrayList<>();

        if (currentlyHistory) {
            rows.add(List.of(InterfaceFactory.createButton("\uD83D\uDD34 Активные", BotConstants.CALLBACK_ORDERS_ACTIVE)));
        } else {
            rows.add(List.of(InterfaceFactory.createButton("\uD83D\uDCD6 История", BotConstants.CALLBACK_ORDERS_HISTORY)));
        }
        rows.add(List.of(InterfaceFactory.createButton(BotConstants.BUTTON_BACK_TO_MAIN, BotConstants.CALLBACK_BACK_TO_MAIN)));

        keyboard.setKeyboard(rows);
        return keyboard;
    }
}
