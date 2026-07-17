package ru.rapidcoder.trader.core.sevice.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.rapidcoder.trader.core.service.MarketDataService;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.GetOrderBookResponse;
import ru.tinkoff.piapi.contract.v1.GetTradingStatusResponse;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.InstrumentShort;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.InstrumentsService;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MarketDataServiceTest {

    private InvestApi investApi;
    private ru.tinkoff.piapi.core.MarketDataService sdkMarketDataService;
    private InstrumentsService instrumentsService;
    private MarketDataService service;

    @BeforeEach
    void setUp() {
        investApi = mock(InvestApi.class);
        sdkMarketDataService = mock(ru.tinkoff.piapi.core.MarketDataService.class);
        instrumentsService = mock(InstrumentsService.class);
        when(investApi.getMarketDataService()).thenReturn(sdkMarketDataService);
        when(investApi.getInstrumentsService()).thenReturn(instrumentsService);
        service = new MarketDataService(investApi);
    }

    @Test
    @DisplayName("getCandles: Должен вызвать API при первом запросе (cache miss)")
    void getCandlesFirstCallHitsApi() {
        String figi = "BBG0013HGFT4";
        Instant from = Instant.now().minusSeconds(86400);
        Instant to = Instant.now();
        List<HistoricCandle> candles = List.of(mock(HistoricCandle.class));

        when(sdkMarketDataService.getCandles(figi, from, to, CandleInterval.CANDLE_INTERVAL_DAY))
                .thenReturn(CompletableFuture.completedFuture(candles));

        List<HistoricCandle> result = service.getCandles(figi, from, to, CandleInterval.CANDLE_INTERVAL_DAY)
                .join();

        assertEquals(candles, result);
        verify(sdkMarketDataService, times(1)).getCandles(figi, from, to, CandleInterval.CANDLE_INTERVAL_DAY);
    }

    @Test
    @DisplayName("getCandles: Должен вернуть кешированный результат при повторном запросе (cache hit)")
    void getCandlesSecondCallFromCache() {
        String figi = "BBG0013HGFT4";
        Instant from = Instant.now().minusSeconds(86400);
        Instant to = Instant.now();
        List<HistoricCandle> candles = List.of(mock(HistoricCandle.class));

        when(sdkMarketDataService.getCandles(figi, from, to, CandleInterval.CANDLE_INTERVAL_DAY))
                .thenReturn(CompletableFuture.completedFuture(candles));

        service.getCandles(figi, from, to, CandleInterval.CANDLE_INTERVAL_DAY).join();
        List<HistoricCandle> result2 = service.getCandles(figi, from, to, CandleInterval.CANDLE_INTERVAL_DAY).join();

        assertEquals(candles, result2);
        verify(sdkMarketDataService, times(1)).getCandles(figi, from, to, CandleInterval.CANDLE_INTERVAL_DAY);
    }

    @Test
    @DisplayName("getLastPrices: Должен вызвать API при первом запросе")
    void getLastPricesFirstCallHitsApi() {
        String figi = "BBG0013HGFT4";
        List<LastPrice> prices = List.of(mock(LastPrice.class));

        when(sdkMarketDataService.getLastPrices(List.of(figi)))
                .thenReturn(CompletableFuture.completedFuture(prices));

        List<LastPrice> result = service.getLastPrices(figi).join();

        assertEquals(prices, result);
        verify(sdkMarketDataService, times(1)).getLastPrices(List.of(figi));
    }

    @Test
    @DisplayName("getLastPrices: Должен вернуть кешированный результат при повторном запросе")
    void getLastPricesSecondCallFromCache() {
        String figi = "BBG0013HGFT4";
        List<LastPrice> prices = List.of(mock(LastPrice.class));

        when(sdkMarketDataService.getLastPrices(List.of(figi)))
                .thenReturn(CompletableFuture.completedFuture(prices));

        service.getLastPrices(figi).join();
        List<LastPrice> result2 = service.getLastPrices(figi).join();

        assertEquals(prices, result2);
        verify(sdkMarketDataService, times(1)).getLastPrices(List.of(figi));
    }

    @Test
    @DisplayName("getOrderBook: Должен вызвать API при первом запросе")
    void getOrderBookFirstCallHitsApi() {
        String figi = "BBG0013HGFT4";
        int depth = 10;
        GetOrderBookResponse response = mock(GetOrderBookResponse.class);

        when(sdkMarketDataService.getOrderBook(figi, depth))
                .thenReturn(CompletableFuture.completedFuture(response));

        GetOrderBookResponse result = service.getOrderBook(figi, depth).join();

        assertEquals(response, result);
        verify(sdkMarketDataService, times(1)).getOrderBook(figi, depth);
    }

    @Test
    @DisplayName("getOrderBook: Должен вернуть кешированный результат при повторном запросе")
    void getOrderBookSecondCallFromCache() {
        String figi = "BBG0013HGFT4";
        int depth = 10;
        GetOrderBookResponse response = mock(GetOrderBookResponse.class);

        when(sdkMarketDataService.getOrderBook(figi, depth))
                .thenReturn(CompletableFuture.completedFuture(response));

        service.getOrderBook(figi, depth).join();
        GetOrderBookResponse result2 = service.getOrderBook(figi, depth).join();

        assertEquals(response, result2);
        verify(sdkMarketDataService, times(1)).getOrderBook(figi, depth);
    }

    @Test
    @DisplayName("getTradingStatus: Должен вызвать API при первом запросе")
    void getTradingStatusFirstCallHitsApi() {
        String figi = "BBG0013HGFT4";
        GetTradingStatusResponse response = mock(GetTradingStatusResponse.class);

        when(sdkMarketDataService.getTradingStatus(figi))
                .thenReturn(CompletableFuture.completedFuture(response));

        GetTradingStatusResponse result = service.getTradingStatus(figi).join();

        assertEquals(response, result);
        verify(sdkMarketDataService, times(1)).getTradingStatus(figi);
    }

    @Test
    @DisplayName("getTradingStatus: Должен вернуть кешированный результат при повторном запросе")
    void getTradingStatusSecondCallFromCache() {
        String figi = "BBG0013HGFT4";
        GetTradingStatusResponse response = mock(GetTradingStatusResponse.class);

        when(sdkMarketDataService.getTradingStatus(figi))
                .thenReturn(CompletableFuture.completedFuture(response));

        service.getTradingStatus(figi).join();
        GetTradingStatusResponse result2 = service.getTradingStatus(figi).join();

        assertEquals(response, result2);
        verify(sdkMarketDataService, times(1)).getTradingStatus(figi);
    }

    @Test
    @DisplayName("findInstrumentByTicker: Должен найти инструмент по тикеру")
    void findInstrumentByTickerFound() {
        String ticker = "AAPL";
        InstrumentShort instrument = mock(InstrumentShort.class);
        when(instrument.getTicker()).thenReturn("AAPL");

        when(instrumentsService.findInstrument(ticker))
                .thenReturn(CompletableFuture.completedFuture(List.of(instrument)));

        var result = service.findInstrumentByTicker(ticker).join();

        assertTrue(result.isPresent());
        assertEquals(instrument, result.get());
        verify(instrumentsService, times(1)).findInstrument(ticker);
    }

    @Test
    @DisplayName("findInstrumentByTicker: Должен вернуть empty, если тикер не совпадает")
    void findInstrumentByTickerNotFound() {
        String ticker = "AAPL";
        InstrumentShort instrument = mock(InstrumentShort.class);
        when(instrument.getTicker()).thenReturn("GOOGL");

        when(instrumentsService.findInstrument(ticker))
                .thenReturn(CompletableFuture.completedFuture(List.of(instrument)));

        var result = service.findInstrumentByTicker(ticker).join();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findInstrumentByTicker: Должен вернуть empty, если список пуст")
    void findInstrumentByTickerEmptyList() {
        String ticker = "AAPL";

        when(instrumentsService.findInstrument(ticker))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        var result = service.findInstrumentByTicker(ticker).join();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getCandles: Разные ключи кеша не пересекаются")
    void getCandlesDifferentKeysNotCached() {
        String figi1 = "BBG0013HGFT4";
        String figi2 = "BBG0044KXFF9";
        Instant from = Instant.now().minusSeconds(86400);
        Instant to = Instant.now();
        List<HistoricCandle> candles1 = List.of(mock(HistoricCandle.class));
        List<HistoricCandle> candles2 = List.of(mock(HistoricCandle.class), mock(HistoricCandle.class));

        when(sdkMarketDataService.getCandles(figi1, from, to, CandleInterval.CANDLE_INTERVAL_DAY))
                .thenReturn(CompletableFuture.completedFuture(candles1));
        when(sdkMarketDataService.getCandles(figi2, from, to, CandleInterval.CANDLE_INTERVAL_DAY))
                .thenReturn(CompletableFuture.completedFuture(candles2));

        List<HistoricCandle> result1 = service.getCandles(figi1, from, to, CandleInterval.CANDLE_INTERVAL_DAY).join();
        List<HistoricCandle> result2 = service.getCandles(figi2, from, to, CandleInterval.CANDLE_INTERVAL_DAY).join();

        assertEquals(candles1, result1);
        assertEquals(candles2, result2);
        verify(sdkMarketDataService, times(1)).getCandles(figi1, from, to, CandleInterval.CANDLE_INTERVAL_DAY);
        verify(sdkMarketDataService, times(1)).getCandles(figi2, from, to, CandleInterval.CANDLE_INTERVAL_DAY);
    }

    @Test
    @DisplayName("getCandles: Разные интервалы не пересекаются в кеше")
    void getCandlesDifferentIntervalsNotCached() {
        String figi = "BBG0013HGFT4";
        Instant from = Instant.now().minusSeconds(86400);
        Instant to = Instant.now();
        List<HistoricCandle> candles1 = List.of(mock(HistoricCandle.class));
        List<HistoricCandle> candles2 = List.of(mock(HistoricCandle.class), mock(HistoricCandle.class));

        when(sdkMarketDataService.getCandles(figi, from, to, CandleInterval.CANDLE_INTERVAL_DAY))
                .thenReturn(CompletableFuture.completedFuture(candles1));
        when(sdkMarketDataService.getCandles(figi, from, to, CandleInterval.CANDLE_INTERVAL_HOUR))
                .thenReturn(CompletableFuture.completedFuture(candles2));

        List<HistoricCandle> result1 = service.getCandles(figi, from, to, CandleInterval.CANDLE_INTERVAL_DAY).join();
        List<HistoricCandle> result2 = service.getCandles(figi, from, to, CandleInterval.CANDLE_INTERVAL_HOUR).join();

        assertEquals(candles1, result1);
        assertEquals(candles2, result2);
    }
}
