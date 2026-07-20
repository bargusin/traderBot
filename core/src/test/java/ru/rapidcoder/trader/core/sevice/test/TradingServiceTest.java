package ru.rapidcoder.trader.core.sevice.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.rapidcoder.trader.core.service.TradingService;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.OrdersService;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TradingServiceTest {

    private InvestApi investApi;
    private OrdersService ordersService;
    private TradingService service;

    @BeforeEach
    void setUp() {
        investApi = mock(InvestApi.class);
        ordersService = mock(OrdersService.class);
        when(investApi.getOrdersService()).thenReturn(ordersService);
        service = new TradingService(investApi);
    }

    @Test
    @DisplayName("placeOrder: Должен вызвать postOrder и вернуть ответ")
    void placeOrderSuccess() {
        String accountId = "test-account";
        String figi = "BBG0013HGFT4";
        long lots = 1;
        Quotation price = Quotation.getDefaultInstance();
        OrderDirection direction = OrderDirection.ORDER_DIRECTION_BUY;
        OrderType orderType = OrderType.ORDER_TYPE_MARKET;

        PostOrderResponse response = mock(PostOrderResponse.class);
        when(ordersService.postOrder(eq(figi), eq(lots), eq(price), eq(direction), eq(accountId), eq(orderType), anyString()))
                .thenReturn(CompletableFuture.completedFuture(response));

        PostOrderResponse result = service.placeOrder(accountId, figi, lots, price, direction, orderType).join();

        assertEquals(response, result);
        verify(ordersService, times(1)).postOrder(eq(figi), eq(lots), eq(price), eq(direction), eq(accountId), eq(orderType), anyString());
    }

    @Test
    @DisplayName("placeLimitOrder: Должен вызвать postLimitOrder и вернуть ответ")
    void placeLimitOrderSuccess() {
        String accountId = "test-account";
        String figi = "BBG0013HGFT4";
        long lots = 1;
        Quotation price = Quotation.newBuilder().setUnits(100).setNano(0).build();
        OrderDirection direction = OrderDirection.ORDER_DIRECTION_BUY;
        TimeInForceType timeInForce = TimeInForceType.TIME_IN_FORCE_DAY;

        PostOrderResponse response = mock(PostOrderResponse.class);
        when(ordersService.postLimitOrder(eq(figi), eq(lots), eq(price), eq(direction), eq(accountId), eq(timeInForce), anyString()))
                .thenReturn(CompletableFuture.completedFuture(response));

        PostOrderResponse result = service.placeLimitOrder(accountId, figi, lots, price, direction, timeInForce).join();

        assertEquals(response, result);
    }

    @Test
    @DisplayName("cancelOrder: Должен вызвать cancelOrder и вернуть время отмены")
    void cancelOrderSuccess() {
        String accountId = "test-account";
        String orderId = "test-order-id";
        Instant cancelTime = Instant.now();

        when(ordersService.cancelOrder(accountId, orderId))
                .thenReturn(CompletableFuture.completedFuture(cancelTime));

        Instant result = service.cancelOrder(accountId, orderId).join();

        assertEquals(cancelTime, result);
        verify(ordersService, times(1)).cancelOrder(accountId, orderId);
    }

    @Test
    @DisplayName("getActiveOrders: Должен вернуть только неисполненные ордера")
    void getActiveOrdersFiltersExecuted() {
        String accountId = "test-account";

        OrderState activeOrder = mock(OrderState.class);
        when(activeOrder.getExecutionReportStatus()).thenReturn(OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW);

        OrderState filledOrder = mock(OrderState.class);
        when(filledOrder.getExecutionReportStatus()).thenReturn(OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_FILL);

        OrderState cancelledOrder = mock(OrderState.class);
        when(cancelledOrder.getExecutionReportStatus()).thenReturn(OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_CANCELLED);

        when(ordersService.getOrders(accountId))
                .thenReturn(CompletableFuture.completedFuture(List.of(activeOrder, filledOrder, cancelledOrder)));

        List<OrderState> result = service.getActiveOrders(accountId).join();

        assertEquals(1, result.size());
        assertEquals(activeOrder, result.get(0));
    }

    @Test
    @DisplayName("getAllOrders: Должен вернуть все ордера")
    void getAllOrdersReturnsAll() {
        String accountId = "test-account";

        OrderState order1 = mock(OrderState.class);
        OrderState order2 = mock(OrderState.class);

        when(ordersService.getOrders(accountId))
                .thenReturn(CompletableFuture.completedFuture(List.of(order1, order2)));

        List<OrderState> result = service.getAllOrders(accountId).join();

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getOrderState: Должен вызвать getOrderState и вернуть статус")
    void getOrderStateSuccess() {
        String accountId = "test-account";
        String orderId = "test-order-id";

        OrderState state = mock(OrderState.class);
        when(ordersService.getOrderState(accountId, orderId))
                .thenReturn(CompletableFuture.completedFuture(state));

        OrderState result = service.getOrderState(accountId, orderId).join();

        assertEquals(state, result);
    }

    @Test
    @DisplayName("getOrderPrice: Должен вызвать getOrderPrice и вернуть расчёт")
    void getOrderPriceSuccess() {
        String accountId = "test-account";
        String figi = "BBG0013HGFT4";
        long lots = 10;
        Quotation price = Quotation.newBuilder().setUnits(100).setNano(0).build();
        OrderDirection direction = OrderDirection.ORDER_DIRECTION_BUY;

        GetOrderPriceResponse response = mock(GetOrderPriceResponse.class);
        when(ordersService.getOrderPrice(accountId, figi, lots, price, direction))
                .thenReturn(CompletableFuture.completedFuture(response));

        GetOrderPriceResponse result = service.getOrderPrice(accountId, figi, lots, price, direction).join();

        assertEquals(response, result);
    }

    @Test
    @DisplayName("getMaxLots: Должен вызвать getMaxLots и вернуть макс. лоты")
    void getMaxLotsSuccess() {
        String accountId = "test-account";
        String figi = "BBG0013HGFT4";
        Quotation price = Quotation.newBuilder().setUnits(100).setNano(0).build();

        GetMaxLotsResponse response = mock(GetMaxLotsResponse.class);
        when(ordersService.getMaxLots(accountId, figi, price))
                .thenReturn(CompletableFuture.completedFuture(response));

        GetMaxLotsResponse result = service.getMaxLots(accountId, figi, price).join();

        assertEquals(response, result);
    }

    @Test
    @DisplayName("mapErrorToMessage: Должен вернуть сообщение о недостатке средств")
    void mapErrorInsufficientFunds() {
        RuntimeException ex = new RuntimeException("10000 insufficient funds");
        String msg = TradingService.mapErrorToMessage(ex);
        assertTrue(msg.contains("Недостаточно средств"));
    }

    @Test
    @DisplayName("mapErrorToMessage: Должен вернуть сообщение о некорректных лотах")
    void mapErrorInvalidLots() {
        RuntimeException ex = new RuntimeException("10006 invalid lot");
        String msg = TradingService.mapErrorToMessage(ex);
        assertTrue(msg.contains("Некорректное количество лотов"));
    }

    @Test
    @DisplayName("mapErrorToMessage: Должен вернуть сообщение об ошибке инструмента")
    void mapErrorInstrumentNotFound() {
        RuntimeException ex = new RuntimeException("10014 instrument not found");
        String msg = TradingService.mapErrorToMessage(ex);
        assertTrue(msg.contains("Инструмент не найден"));
    }

    @Test
    @DisplayName("mapErrorToMessage: Должен вернуть сообщение об ошибке счёта")
    void mapErrorAccountNotFound() {
        RuntimeException ex = new RuntimeException("10015 account not found");
        String msg = TradingService.mapErrorToMessage(ex);
        assertTrue(msg.contains("Счёт не найден"));
    }

    @Test
    @DisplayName("mapErrorToMessage: Должен вернуть сообщение об ошибке ордера")
    void mapErrorOrderNotFound() {
        RuntimeException ex = new RuntimeException("10012 order not found");
        String msg = TradingService.mapErrorToMessage(ex);
        assertTrue(msg.contains("Ордер не найден"));
    }

    @Test
    @DisplayName("mapErrorToMessage: Должен вернуть generic сообщение для неизвестной ошибки")
    void mapErrorGeneric() {
        RuntimeException ex = new RuntimeException("unknown error");
        String msg = TradingService.mapErrorToMessage(ex);
        assertTrue(msg.contains("Ошибка: unknown error"));
    }

    @Test
    @DisplayName("mapErrorToMessage: Должен вернуть default сообщение для null message")
    void mapErrorNullMessage() {
        RuntimeException ex = new RuntimeException((String) null);
        String msg = TradingService.mapErrorToMessage(ex);
        assertEquals("Неизвестная ошибка", msg);
    }
}
