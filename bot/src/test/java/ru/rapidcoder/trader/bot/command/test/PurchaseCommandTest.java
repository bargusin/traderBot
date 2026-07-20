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
import ru.rapidcoder.trader.bot.command.trading.PurchaseCommand;
import ru.rapidcoder.trader.core.service.TradingMode;
import ru.rapidcoder.trader.core.service.TradingSessionManager;
import ru.tinkoff.piapi.contract.v1.GetMaxLotsResponse;
import ru.tinkoff.piapi.contract.v1.InstrumentShort;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.InstrumentsService;
import ru.tinkoff.piapi.core.MarketDataService;
import ru.tinkoff.piapi.core.OperationsService;
import ru.tinkoff.piapi.core.OrdersService;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PurchaseCommandTest {

    @Mock
    private Bot bot;

    @Mock
    private TradingSessionManager tradingSessionManager;

    @Mock
    private InvestApi investApi;

    @Mock
    private MarketDataService sdkMarketDataService;

    @Mock
    private InstrumentsService instrumentsService;

    @Mock
    private OrdersService ordersService;

    @Mock
    private OperationsService operationsService;

    private PurchaseCommand command;

    @BeforeEach
    void setUp() {
        command = new PurchaseCommand(bot, "/purchase", "Купить инструмент");
        when(bot.getTradingSessionManager()).thenReturn(tradingSessionManager);
        when(tradingSessionManager.getApi(anyLong())).thenReturn(investApi);
        when(tradingSessionManager.getCurrentAccountId(anyLong())).thenReturn("test-account");
        when(tradingSessionManager.getCurrentMode(anyLong())).thenReturn(TradingMode.SANDBOX);
        when(investApi.getMarketDataService()).thenReturn(sdkMarketDataService);
        when(investApi.getInstrumentsService()).thenReturn(instrumentsService);
        when(investApi.getOrdersService()).thenReturn(ordersService);
        when(investApi.getOperationsService()).thenReturn(operationsService);
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

    @Test
    @DisplayName("Должен запросить тикер, если аргумент не передан")
    void emptyTickerPromptsInput() {
        Update update = createMessageUpdate("/purchase");

        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendMessage(eq(123L), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Введите тикер"));
    }

    @Test
    @DisplayName("Должен показать ошибку, если счет не определен")
    void noAccountShowsError() {
        when(tradingSessionManager.getCurrentAccountId(123L)).thenReturn("");

        Update update = createMessageUpdate("/purchase AAPL");
        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendMessage(eq(123L), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Счет не определен"));
    }

    @Test
    @DisplayName("Должен показать ошибку, если инструмент не найден")
    void instrumentNotFound() {
        when(instrumentsService.findInstrument("UNKNOWN"))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        Update update = createMessageUpdate("/purchase UNKNOWN");
        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendMessage(eq(123L), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("не найден"));
    }

    @Test
    @DisplayName("Должен показать информацию об инструменте и запросить количество")
    void showsInstrumentInfoAndPromptsQuantity() {
        InstrumentShort instrument = mock(InstrumentShort.class);
        when(instrument.getFigi()).thenReturn("BBG0013HGFT4");
        when(instrument.getTicker()).thenReturn("AAPL");
        when(instrument.getName()).thenReturn("Apple Inc.");

        when(instrumentsService.findInstrument("AAPL"))
                .thenReturn(CompletableFuture.completedFuture(List.of(instrument)));

        GetMaxLotsResponse maxLotsResponse = mock(GetMaxLotsResponse.class);
        GetMaxLotsResponse.BuyLimitsView buyLimits = mock(GetMaxLotsResponse.BuyLimitsView.class);
        when(buyLimits.getBuyMaxLots()).thenReturn(100L);
        when(maxLotsResponse.getBuyLimits()).thenReturn(buyLimits);
        when(ordersService.getMaxLots(eq("test-account"), eq("BBG0013HGFT4"), any(Quotation.class)))
                .thenReturn(CompletableFuture.completedFuture(maxLotsResponse));

        LastPrice lastPrice = mock(LastPrice.class);
        Quotation price = Quotation.newBuilder().setUnits(150).setNano(250000000).build();
        when(lastPrice.getPrice()).thenReturn(price);
        when(sdkMarketDataService.getLastPrices(List.of("BBG0013HGFT4")))
                .thenReturn(CompletableFuture.completedFuture(List.of(lastPrice)));

        Update update = createMessageUpdate("/purchase AAPL");
        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendMessage(eq(123L), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Apple Inc."));
        assertTrue(text.contains("AAPL"));
        assertTrue(text.contains("Макс. лотов к покупке: 100"));
        assertTrue(text.contains("Введите количество лотов"));
    }

    @Test
    @DisplayName("Должен показать ошибку при исключении API")
    void showsErrorOnApiException() {
        when(instrumentsService.findInstrument("ERR"))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API error")));

        Update update = createMessageUpdate("/purchase ERR");
        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendMessage(eq(123L), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Ошибка"));
    }
}
