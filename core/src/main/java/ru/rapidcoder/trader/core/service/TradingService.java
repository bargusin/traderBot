package ru.rapidcoder.trader.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TradingService {

    private static final Logger logger = LoggerFactory.getLogger(TradingService.class);

    private final InvestApi api;

    public TradingService(InvestApi api) {
        this.api = api;
    }

    public CompletableFuture<PostOrderResponse> placeOrder(String accountId, String figi, long lots,
                                                           Quotation price, OrderDirection direction,
                                                           OrderType orderType) {
        String orderId = UUID.randomUUID().toString();
        logger.info("Размещение ордера: accountId={}, figi={}, lots={}, direction={}, orderType={}, orderId={}",
                accountId, figi, lots, direction, orderType, orderId);
        try {
            return api.getOrdersService()
                    .postOrder(figi, lots, price, direction, accountId, orderType, orderId)
                    .handle((response, ex) -> {
                        if (ex != null) {
                            logger.error("Ошибка API при размещении ордера: accountId={}, figi={}, ошибка={}",
                                    accountId, figi, ex.getMessage());
                        } else {
                            logger.info("Ордер исполнен: orderId={}, status={}, lotsExecuted={}",
                                    response.getOrderId(), response.getExecutionReportStatus(), response.getLotsExecuted());
                        }
                        if (ex != null) {
                            throw new java.util.concurrent.CompletionException(ex);
                        }
                        return response;
                    });
        } catch (ApiRuntimeException e) {
            logger.error("Ошибка размещения ордера для {}: {}", figi, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<PostOrderResponse> placeLimitOrder(String accountId, String figi, long lots,
                                                                Quotation price, OrderDirection direction,
                                                                TimeInForceType timeInForce) {
        String orderId = UUID.randomUUID().toString();
        try {
            return api.getOrdersService()
                    .postLimitOrder(figi, lots, price, direction, accountId, timeInForce, orderId);
        } catch (ApiRuntimeException e) {
            logger.error("Ошибка размещения лимитного ордера для {}: {}", figi, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<Instant> cancelOrder(String accountId, String orderId) {
        try {
            return api.getOrdersService()
                    .cancelOrder(accountId, orderId);
        } catch (ApiRuntimeException e) {
            logger.error("Ошибка отмены ордера {}: {}", orderId, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<List<OrderState>> getActiveOrders(String accountId) {
        try {
            return api.getOrdersService()
                    .getOrders(accountId)
                    .thenApply(orders -> orders.stream()
                            .filter(o -> o.getExecutionReportStatus() != OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_FILL
                                    && o.getExecutionReportStatus() != OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_REJECTED
                                    && o.getExecutionReportStatus() != OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_CANCELLED)
                            .toList());
        } catch (ApiRuntimeException e) {
            logger.error("Ошибка получения активных ордеров: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<List<OrderState>> getAllOrders(String accountId) {
        try {
            return api.getOrdersService()
                    .getOrders(accountId);
        } catch (ApiRuntimeException e) {
            logger.error("Ошибка получения ордеров: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<OrderState> getOrderState(String accountId, String orderId) {
        try {
            return api.getOrdersService()
                    .getOrderState(accountId, orderId);
        } catch (ApiRuntimeException e) {
            logger.error("Ошибка получения статуса ордера {}: {}", orderId, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<GetOrderPriceResponse> getOrderPrice(String accountId, String figi, long lots,
                                                                   Quotation price, OrderDirection direction) {
        try {
            return api.getOrdersService()
                    .getOrderPrice(accountId, figi, lots, price, direction);
        } catch (ApiRuntimeException e) {
            logger.error("Ошибка расчёта стоимости ордера для {}: {}", figi, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<GetMaxLotsResponse> getMaxLots(String accountId, String figi, Quotation price) {
        try {
            return api.getOrdersService()
                    .getMaxLots(accountId, figi, price);
        } catch (ApiRuntimeException e) {
            logger.error("Ошибка получения макс. лотов для {}: {}", figi, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public static String mapErrorToMessage(Throwable e) {
        String msg = e.getMessage();
        if (msg == null) {
            return "Неизвестная ошибка";
        }
        if (msg.contains("10000") || msg.contains("insufficient")) {
            return "Недостаточно средств на счёте";
        }
        if (msg.contains("10006") || msg.contains("invalid lot")) {
            return "Некорректное количество лотов";
        }
        if (msg.contains("10014") || msg.contains("instrument not found")) {
            return "Инструмент не найден";
        }
        if (msg.contains("10015") || msg.contains("account not found")) {
            return "Счёт не найден";
        }
        if (msg.contains("10012") || msg.contains("order not found")) {
            return "Ордер не найден или уже исполнен";
        }
        if (msg.contains("70001")) {
            return "Внутренняя ошибка сервиса (70001). Возможные причины:\n"
                    + "• Токен не имеет прав на торговлю (read-only)\n"
                    + "• Недостаточно средств на счёте\n"
                    + "• Инструмент недоступен для торговли\n"
                    + "• Неверный ID счета\n\n"
                    + "Проверьте:\n"
                    + "1. Токен в /settings должен быть full access\n"
                    + "2. Аккаунт выбран через /account\n"
                    + "3. Баланс достаточен для покупки";
        }
        if (msg.contains("70002")) {
            return "Инструмент не поддерживает данный тип ордера";
        }
        if (msg.contains("70003")) {
            return "Недостаточно лотов для исполнения ордера";
        }
        if (msg.contains("70004")) {
            return "Ордер не может быть исполнен по текущей цене";
        }
        if (msg.contains("70005")) {
            return "Превышен лимит на количество активных ордеров";
        }
        return "Ошибка: " + msg;
    }
}
