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
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.command.PriceCommand;
import ru.rapidcoder.trader.core.service.TradingSessionManager;
import ru.tinkoff.piapi.contract.v1.GetTradingStatusResponse;
import ru.tinkoff.piapi.contract.v1.InstrumentShort;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.contract.v1.Quotation;
import ru.tinkoff.piapi.contract.v1.SecurityTradingStatus;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.InstrumentsService;
import ru.tinkoff.piapi.core.MarketDataService;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PriceCommandTest {

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

    private PriceCommand command;

    @BeforeEach
    void setUp() {
        command = new PriceCommand(bot, "/price", "Показать цену");
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

    @Test
    @DisplayName("Должен показать.usage, если тикер не передан")
    void emptyTickerShowsUsage() {
        Update update = createMessageUpdate("/price");

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

        Update update = createMessageUpdate("/price UNKNOWN");
        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendMessage(eq(123L), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("не найден"));
    }

    @Test
    @DisplayName("Должен показать цену инструмента с открытыми торгами")
    void showsPriceWithOpenStatus() {
        InstrumentShort instrument = mock(InstrumentShort.class);
        when(instrument.getFigi()).thenReturn("BBG0013HGFT4");
        when(instrument.getTicker()).thenReturn("AAPL");
        when(instrument.getName()).thenReturn("Apple Inc.");

        when(instrumentsService.findInstrument("AAPL"))
                .thenReturn(CompletableFuture.completedFuture(List.of(instrument)));

        LastPrice lastPrice = mock(LastPrice.class);
        Quotation price = MapperUtils.bigDecimalToQuotation(new BigDecimal("150.25"));
        when(lastPrice.getPrice()).thenReturn(price);
        when(lastPrice.getTime()).thenReturn(com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(Instant.now().getEpochSecond())
                .build());
        when(sdkMarketDataService.getLastPrices(List.of("BBG0013HGFT4")))
                .thenReturn(CompletableFuture.completedFuture(List.of(lastPrice)));

        GetTradingStatusResponse statusResponse = mock(GetTradingStatusResponse.class);
        when(statusResponse.getTradingStatus()).thenReturn(SecurityTradingStatus.SECURITY_TRADING_STATUS_NORMAL_TRADING);
        when(sdkMarketDataService.getTradingStatus("BBG0013HGFT4"))
                .thenReturn(CompletableFuture.completedFuture(statusResponse));

        Update update = createMessageUpdate("/price AAPL");
        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendMessage(eq(123L), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Apple Inc."));
        assertTrue(text.contains("AAPL"));
        assertTrue(text.contains("150.25"));
        assertTrue(text.contains("Торги открыты"));
    }

    @Test
    @DisplayName("Должен показать цену с закрытыми торгами")
    void showsPriceWithClosedStatus() {
        InstrumentShort instrument = mock(InstrumentShort.class);
        when(instrument.getFigi()).thenReturn("BBG0013HGFT4");
        when(instrument.getTicker()).thenReturn("SBER");
        when(instrument.getName()).thenReturn("Сбербанк");

        when(instrumentsService.findInstrument("SBER"))
                .thenReturn(CompletableFuture.completedFuture(List.of(instrument)));

        LastPrice lastPrice = mock(LastPrice.class);
        Quotation price = MapperUtils.bigDecimalToQuotation(new BigDecimal("280.50"));
        when(lastPrice.getPrice()).thenReturn(price);
        when(lastPrice.getTime()).thenReturn(com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(Instant.now().getEpochSecond())
                .build());
        when(sdkMarketDataService.getLastPrices(List.of("BBG0013HGFT4")))
                .thenReturn(CompletableFuture.completedFuture(List.of(lastPrice)));

        GetTradingStatusResponse statusResponse = mock(GetTradingStatusResponse.class);
        when(statusResponse.getTradingStatus()).thenReturn(SecurityTradingStatus.SECURITY_TRADING_STATUS_NOT_AVAILABLE_FOR_TRADING);
        when(sdkMarketDataService.getTradingStatus("BBG0013HGFT4"))
                .thenReturn(CompletableFuture.completedFuture(statusResponse));

        Update update = createMessageUpdate("/price SBER");
        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendMessage(eq(123L), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Торги закрыты"));
        assertTrue(text.contains("280.50"));
    }

    @Test
    @DisplayName("Должен показать цену, если список цен пуст")
    void showsPriceWhenNoPrices() {
        InstrumentShort instrument = mock(InstrumentShort.class);
        when(instrument.getFigi()).thenReturn("BBG0013HGFT4");
        when(instrument.getTicker()).thenReturn("TCSG");
        when(instrument.getName()).thenReturn("Т-Банк");

        when(instrumentsService.findInstrument("TCSG"))
                .thenReturn(CompletableFuture.completedFuture(List.of(instrument)));

        when(sdkMarketDataService.getLastPrices(List.of("BBG0013HGFT4")))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        GetTradingStatusResponse statusResponse = mock(GetTradingStatusResponse.class);
        when(statusResponse.getTradingStatus()).thenReturn(SecurityTradingStatus.SECURITY_TRADING_STATUS_NORMAL_TRADING);
        when(sdkMarketDataService.getTradingStatus("BBG0013HGFT4"))
                .thenReturn(CompletableFuture.completedFuture(statusResponse));

        Update update = createMessageUpdate("/price TCSG");
        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendMessage(eq(123L), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Т-Банк"));
        assertFalse(text.contains("Цена:"));
    }

    @Test
    @DisplayName("Должен показать ошибку при исключении API")
    void showsErrorOnApiException() {
        when(instrumentsService.findInstrument("ERR"))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API error")));

        Update update = createMessageUpdate("/price ERR");
        command.execute(update);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(bot).sendMessage(eq(123L), textCaptor.capture(), any(InlineKeyboardMarkup.class));

        String text = textCaptor.getValue();
        assertTrue(text.contains("Ошибка"));
    }
}
