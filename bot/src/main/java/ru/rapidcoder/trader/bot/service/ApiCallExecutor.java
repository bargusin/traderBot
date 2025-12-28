package ru.rapidcoder.trader.bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.core.service.TradingSessionManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

public class ApiCallExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ApiCallExecutor.class);

    private final Bot bot;
    private final TradingSessionManager sessionManager;

    public ApiCallExecutor(Bot bot, TradingSessionManager sessionManager) {
        this.bot = bot;
        this.sessionManager = sessionManager;
    }

    /**
     * Обертка для выполнения API вызовов
     *
     * @param action Функция, возвращающая CompletableFuture (сам запрос к API)
     * @param <T>    Тип возвращаемого значения
     */
    public <T> CompletableFuture<T> execute(Supplier<CompletableFuture<T>> action) {
        return action.get()
                .handle((result, ex) -> {
                    if (ex == null) {
                        return result;
                    }
                    if (ex instanceof CompletionException) {
                        throw (CompletionException) ex;
                    }
                    throw new CompletionException(ex);
                });
    }
}
