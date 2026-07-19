package ru.rapidcoder.trader.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.GetOrderBookResponse;
import ru.tinkoff.piapi.contract.v1.GetTradingStatusResponse;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.contract.v1.InstrumentShort;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);

    private static final long TTL_PRICES_MS = 60_000;
    private static final long TTL_CANDLES_MS = 300_000;
    private static final long TTL_ORDERBOOK_MS = 600_000;
    private static final int MAX_CACHE_SIZE = 500;

    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();
    private final Deque<String> cacheOrder = new ArrayDeque<>();

    private final InvestApi api;

    public MarketDataService(InvestApi api) {
        this.api = api;
    }

    public CompletableFuture<List<HistoricCandle>> getCandles(String figi, Instant from, Instant to, CandleInterval interval) {
        String key = "candles:" + figi + ":" + from + ":" + to + ":" + interval;
        @SuppressWarnings("unchecked")
        CacheEntry<List<HistoricCandle>> entry = (CacheEntry<List<HistoricCandle>>) cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return CompletableFuture.completedFuture(entry.value());
        }
        try {
            CompletableFuture<List<HistoricCandle>> future = api.getMarketDataService()
                    .getCandles(figi, from, to, interval);
            future.thenAccept(result -> putCache(key, result, TTL_CANDLES_MS));
            return future;
        } catch (ApiRuntimeException e) {
            logger.error("Ошибка получения свечей для {}: {}", figi, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<List<LastPrice>> getLastPrices(String figi) {
        String key = "prices:" + figi;
        @SuppressWarnings("unchecked")
        CacheEntry<List<LastPrice>> entry = (CacheEntry<List<LastPrice>>) cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return CompletableFuture.completedFuture(entry.value());
        }
        try {
            CompletableFuture<List<LastPrice>> future = api.getMarketDataService()
                    .getLastPrices(List.of(figi));
            future.thenAccept(result -> putCache(key, result, TTL_PRICES_MS));
            return future;
        } catch (ApiRuntimeException e) {
            logger.error("Ошибка получения последних цен для {}: {}", figi, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<GetOrderBookResponse> getOrderBook(String figi, int depth) {
        String key = "orderbook:" + figi + ":" + depth;
        @SuppressWarnings("unchecked")
        CacheEntry<GetOrderBookResponse> entry = (CacheEntry<GetOrderBookResponse>) cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return CompletableFuture.completedFuture(entry.value());
        }
        try {
            CompletableFuture<GetOrderBookResponse> future = api.getMarketDataService()
                    .getOrderBook(figi, depth);
            future.thenAccept(result -> putCache(key, result, TTL_ORDERBOOK_MS));
            return future;
        } catch (ApiRuntimeException e) {
            logger.error("Ошибка получения стакана для {}: {}", figi, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<GetTradingStatusResponse> getTradingStatus(String figi) {
        String key = "status:" + figi;
        @SuppressWarnings("unchecked")
        CacheEntry<GetTradingStatusResponse> entry = (CacheEntry<GetTradingStatusResponse>) cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return CompletableFuture.completedFuture(entry.value());
        }
        try {
            CompletableFuture<GetTradingStatusResponse> future = api.getMarketDataService()
                    .getTradingStatus(figi);
            future.thenAccept(result -> putCache(key, result, TTL_PRICES_MS));
            return future;
        } catch (ApiRuntimeException e) {
            logger.error("Ошибка получения статуса торгов для {}: {}", figi, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<Optional<InstrumentShort>> findInstrumentByTicker(String ticker) {
        try {
            String query = ticker.trim();
            return api.getInstrumentsService()
                    .findInstrument(query)
                    .thenApply(instruments -> instruments.stream()
                            .filter(i -> query.equalsIgnoreCase(i.getTicker())
                                    || query.equalsIgnoreCase(i.getFigi())
                                    || query.equalsIgnoreCase(i.getIsin()))
                            .findFirst());
        } catch (ApiRuntimeException e) {
            logger.error("Ошибка поиска инструмента {}: {}", ticker, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    private synchronized <T> void putCache(String key, T value, long ttlMs) {
        if (cacheOrder.size() >= MAX_CACHE_SIZE) {
            String oldest = cacheOrder.pollFirst();
            if (oldest != null) {
                cache.remove(oldest);
            }
        }
        cache.put(key, CacheEntry.of(value, ttlMs));
        cacheOrder.addLast(key);
    }

    private record CacheEntry<T>(T value, long expiresAt) {
        static <T> CacheEntry<T> of(T value, long ttlMs) {
            return new CacheEntry<>(value, System.currentTimeMillis() + ttlMs);
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
