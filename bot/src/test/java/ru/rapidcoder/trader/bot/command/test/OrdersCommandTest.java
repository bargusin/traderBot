package ru.rapidcoder.trader.bot.command.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.command.BotConstants;
import ru.rapidcoder.trader.bot.command.trading.OrdersCommand;
import ru.rapidcoder.trader.core.service.TradingMode;
import ru.rapidcoder.trader.core.service.TradingSessionManager;
import ru.tinkoff.piapi.contract.v1.OrderDirection;
import ru.tinkoff.piapi.contract.v1.OrderExecutionReportStatus;
import ru.tinkoff.piapi.contract.v1.OrderState;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.OrdersService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OrdersCommandTest {

    @Mock
    private Bot bot;

    @Mock
    private TradingSessionManager tradingSessionManager;

    @Mock
    private InvestApi investApi;

    @Mock
    private OrdersService ordersService;

    private OrdersCommand command;

    @BeforeEach
    void setUp() {
        command = new OrdersCommand(bot, "/orders", "Просмотр ордеров");
        when(bot.getTradingSessionManager()).thenReturn(tradingSessionManager);
        when(tradingSessionManager.getApi(anyLong())).thenReturn(investApi);
        when(tradingSessionManager.getCurrentAccountId(anyLong())).thenReturn("test-account");
        when(tradingSessionManager.getCurrentMode(anyLong())).thenReturn(TradingMode.SANDBOX);
        when(investApi.getOrdersService()).thenReturn(ordersService);
    }

    private Update createMessageUpdate(String text) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        User user = mock(User.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.hasCallbackQuery()).thenReturn(false);
        when(update.getMessage()).thenReturn(message);
        when(message.getText()).thenReturn(text);
        when(message.getChatId()).thenReturn(123L);
        when(message.getFrom()).thenReturn(user);
        when(user.getId()).thenReturn(456L);

        return update;
    }

    private Update createCallbackUpdate(String callbackData) {
        Update update = mock(Update.class);
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        User user = mock(User.class);

        when(update.hasMessage()).thenReturn(false);
        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.getCallbackQuery()).thenReturn(callbackQuery);
        when(callbackQuery.getData()).thenReturn(callbackData);
        when(callbackQuery.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(123L);
        when(message.getMessageId()).thenReturn(100);
        when(callbackQuery.getFrom()).thenReturn(user);
        when(user.getId()).thenReturn(456L);

        return update;
    }

    @Test
    @DisplayName("Должен показать ошибку, если счет не определен")
    void noAccountShowsError() {
        when(tradingSessionManager.getCurrentAccountId(123L)).thenReturn("");

        Update update = createMessageUpdate("/orders");
        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendMessage(eq(123L), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Счет не определен"));
    }

    @Test
    @DisplayName("Должен показать сообщение об отсутствии активных ордеров")
    void noActiveOrdersShowsMessage() {
        when(ordersService.getOrders("test-account"))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        Update update = createMessageUpdate("/orders");
        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendMessage(eq(123L), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Активных ордеров нет"));
    }

    @Test
    @DisplayName("Должен показать активные ордера")
    void showsActiveOrders() {
        OrderState activeOrder = mock(OrderState.class);
        when(activeOrder.getOrderId()).thenReturn("order-1");
        when(activeOrder.getExecutionReportStatus()).thenReturn(OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_NEW);
        when(activeOrder.getDirection()).thenReturn(OrderDirection.ORDER_DIRECTION_BUY);
        when(activeOrder.getLotsRequested()).thenReturn(10L);
        when(activeOrder.getLotsExecuted()).thenReturn(0L);

        when(ordersService.getOrders("test-account"))
                .thenReturn(CompletableFuture.completedFuture(List.of(activeOrder)));

        Update update = createMessageUpdate("/orders");
        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendMessage(eq(123L), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Активные ордера"));
        assertTrue(text.contains("order-1"));
        assertTrue(text.contains("ПОКУПКА"));
        assertTrue(text.contains("Новый"));
    }

    @Test
    @DisplayName("Должен переключиться на историю по callback")
    void switchesToHistoryOnCallback() {
        OrderState filledOrder = mock(OrderState.class);
        when(filledOrder.getOrderId()).thenReturn("order-2");
        when(filledOrder.getExecutionReportStatus()).thenReturn(OrderExecutionReportStatus.EXECUTION_REPORT_STATUS_FILL);
        when(filledOrder.getDirection()).thenReturn(OrderDirection.ORDER_DIRECTION_SELL);
        when(filledOrder.getLotsRequested()).thenReturn(5L);
        when(filledOrder.getLotsExecuted()).thenReturn(5L);

        when(ordersService.getOrders("test-account"))
                .thenReturn(CompletableFuture.completedFuture(List.of(filledOrder)));

        Update update = createCallbackUpdate(BotConstants.CALLBACK_ORDERS_HISTORY);
        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).updateMessage(eq(123L), eq(100), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("История ордеров"));
        assertTrue(text.contains("order-2"));
        assertTrue(text.contains("ПРОДАЖА"));
        assertTrue(text.contains("Исполнен"));
    }

    @Test
    @DisplayName("Должен показать ошибку при исключении API")
    void showsErrorOnApiException() {
        when(ordersService.getOrders("test-account"))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API error")));

        Update update = createMessageUpdate("/orders");
        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendMessage(eq(123L), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Ошибка"));
    }
}
