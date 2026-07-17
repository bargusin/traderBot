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
import ru.rapidcoder.trader.bot.command.ChartCommand;
import ru.rapidcoder.trader.core.service.TradingSessionManager;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.InstrumentShort;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.InstrumentsService;
import ru.tinkoff.piapi.core.MarketDataService;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ChartCommandTest {

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

    private ChartCommand command;

    @BeforeEach
    void setUp() {
        command = new ChartCommand(bot, "/chart", "Показать график");
        when(bot.getTradingSessionManager()).thenReturn(tradingSessionManager);
        when(tradingSessionManager.getApi(anyLong())).thenReturn(investApi);
        when(investApi.getMarketDataService()).thenReturn(sdkMarketDataService);
        when(investApi.getInstrumentsService()).thenReturn(instrumentsService);
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
        when(callbackQuery.getFrom()).thenReturn(user);
        when(message.getChatId()).thenReturn(123L);
        when(message.getMessageId()).thenReturn(100);
        when(user.getId()).thenReturn(456L);

        return update;
    }

    private HistoricCandle buildCandle(double open, double close, double high, double low, Instant time) {
        return HistoricCandle.newBuilder()
                .setOpen(MapperUtils.bigDecimalToQuotation(BigDecimal.valueOf(open)))
                .setClose(MapperUtils.bigDecimalToQuotation(BigDecimal.valueOf(close)))
                .setHigh(MapperUtils.bigDecimalToQuotation(BigDecimal.valueOf(high)))
                .setLow(MapperUtils.bigDecimalToQuotation(BigDecimal.valueOf(low)))
                .setTime(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(time.getEpochSecond())
                        .setNanos(time.getNano())
                        .build())
                .setVolume(1000)
                .setIsComplete(true)
                .build();
    }

    @Test
    @DisplayName("Должен показать usage, если тикер не передан")
    void emptyTickerShowsUsage() {
        Update update = createMessageUpdate("/chart");

        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendMessage(eq(123L), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Использование"));
    }

    @Test
    @DisplayName("Должен показать ошибку, если инструмент не найден")
    void instrumentNotFound() {
        when(instrumentsService.findInstrument("UNKNOWN"))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        Update update = createMessageUpdate("/chart UNKNOWN");
        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendMessage(eq(123L), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("не найден"));
    }

    @Test
    @DisplayName("Должен показать график с свечами")
    void showsChartWithCandles() {
        InstrumentShort instrument = mock(InstrumentShort.class);
        when(instrument.getFigi()).thenReturn("BBG0013HGFT4");
        when(instrument.getTicker()).thenReturn("AAPL");
        when(instrument.getName()).thenReturn("Apple Inc.");

        when(instrumentsService.findInstrument("AAPL"))
                .thenReturn(CompletableFuture.completedFuture(List.of(instrument)));

        Instant now = Instant.now();
        List<HistoricCandle> candles = List.of(
                buildCandle(148.0, 150.0, 151.0, 147.0, now.minusSeconds(86400 * 2)),
                buildCandle(150.0, 152.0, 153.0, 149.0, now.minusSeconds(86400)),
                buildCandle(152.0, 155.0, 156.0, 151.0, now)
        );

        when(sdkMarketDataService.getCandles(
                eq("BBG0013HGFT4"),
                any(Instant.class), any(Instant.class),
                eq(CandleInterval.CANDLE_INTERVAL_DAY)))
                .thenReturn(CompletableFuture.completedFuture(candles));

        Update update = createMessageUpdate("/chart AAPL");
        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendMessage(eq(123L), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Apple Inc."));
        assertTrue(text.contains("AAPL"));
        assertTrue(text.contains("155.00"));
        assertTrue(text.contains("Изменение:"));
    }

    @Test
    @DisplayName("Должен показать 'Нет данных', если свечи пустые")
    void emptyCandlesShowsNoData() {
        InstrumentShort instrument = mock(InstrumentShort.class);
        when(instrument.getFigi()).thenReturn("BBG0013HGFT4");
        when(instrument.getTicker()).thenReturn("AAPL");
        when(instrument.getName()).thenReturn("Apple Inc.");

        when(instrumentsService.findInstrument("AAPL"))
                .thenReturn(CompletableFuture.completedFuture(List.of(instrument)));

        when(sdkMarketDataService.getCandles(
                eq("BBG0013HGFT4"),
                any(Instant.class), any(Instant.class),
                eq(CandleInterval.CANDLE_INTERVAL_DAY)))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        Update update = createMessageUpdate("/chart AAPL");
        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendMessage(eq(123L), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Нет данных"));
    }

    @Test
    @DisplayName("Должен обработать callback смены периода")
    void handleCallbackPeriodChange() {
        InstrumentShort instrument = mock(InstrumentShort.class);
        when(instrument.getFigi()).thenReturn("BBG0013HGFT4");
        when(instrument.getTicker()).thenReturn("AAPL");
        when(instrument.getName()).thenReturn("Apple Inc.");

        when(instrumentsService.findInstrument("AAPL"))
                .thenReturn(CompletableFuture.completedFuture(List.of(instrument)));

        Instant now = Instant.now();
        List<HistoricCandle> candles = List.of(
                buildCandle(150.0, 152.0, 153.0, 149.0, now.minusSeconds(3600)),
                buildCandle(152.0, 154.0, 155.0, 151.0, now)
        );

        when(sdkMarketDataService.getCandles(
                eq("BBG0013HGFT4"),
                any(Instant.class), any(Instant.class),
                eq(CandleInterval.CANDLE_INTERVAL_1_MIN)))
                .thenReturn(CompletableFuture.completedFuture(candles));

        Update update = createCallbackUpdate("chart_period#AAPL#CANDLE_INTERVAL_1_MIN");
        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).updateMessage(eq(123L), eq(100), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Apple Inc."));
        assertTrue(text.contains("Внутридневной"));
    }

    @Test
    @DisplayName("Должен показать ошибку при невалидном callback")
    void invalidCallbackShowsError() {
        Update update = createCallbackUpdate("invalid_data");

        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).updateMessage(eq(123L), eq(100), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Ошибка"));
    }

    @Test
    @DisplayName("Должен показать ошибку при исключении API")
    void showsErrorOnApiException() {
        when(instrumentsService.findInstrument("ERR"))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API error")));

        Update update = createMessageUpdate("/chart ERR");
        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendMessage(eq(123L), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Ошибка"));
    }
}
