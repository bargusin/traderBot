package ru.rapidcoder.trader.bot.service;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.core.service.TradingSessionManager;
import ru.tinkoff.piapi.core.exception.ApiRuntimeException;

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
     * @param chatId ID чата пользователя (для уведомлений)
     * @param action Функция, возвращающая CompletableFuture (сам запрос к API)
     * @param <T>    Тип возвращаемого значения
     */
    public <T> CompletableFuture<T> execute(Long chatId, Supplier<CompletableFuture<T>> action) {
        return action.get()
                .handle((result, ex) -> {
                    // 1. Если всё хорошо - просто возвращаем результат
                    if (ex == null) {
                        return result;
                    }

                    // 2. Если ошибка - проверяем, не протух ли токен
                    if (isAuthError(ex)) {
                        handleAuthError(chatId);
                        // Возвращаем null или бросаем специальное исключение, чтобы прервать цепочку
                        throw new CompletionException(new IllegalStateException("Токен невалиден"));
                    }

                    // 3. Если ошибка другая - прокидываем её дальше (пусть команда сама решает)
                    // Разворачиваем CompletionException, если нужно, или кидаем как есть
                    if (ex instanceof CompletionException) {
                        throw (CompletionException) ex;
                    }
                    throw new CompletionException(ex);
                });
    }

    private void handleAuthError(Long chatId) {
        logger.warn("Обнаружен невалидный токен у пользователя {}", chatId);

        // 1. Удаляем "битую" сессию из кеша
        sessionManager.removeSession(chatId);

        // 2. (Опционально) Помечаем в БД, что токен невалиден
        // userRepository.markTokenInvalid(chatId);

        // 3. Отправляем сообщение пользователю с кнопкой ввода нового токена
        String text = "⛔️ <b>Ошибка авторизации</b>\n\nПохоже, срок действия вашего токена истек или он был отозван.\nПожалуйста, выпустите новый токен и введите его заново.";

        // Пример отправки (можно добавить клавиатуру с кнопкой "Обновить токен")
        bot.sendMessage(chatId, text, null);
    }

    /**
     * Проверяет, является ли ошибка проблемой авторизации
     *
     * @param e
     * @return признак ошибки авторизации
     */
    private boolean isAuthError(Throwable e) {
        Throwable cause = e;
        while (cause instanceof CompletionException || cause.getCause() != null && cause != cause.getCause()) {
            if (cause instanceof CompletionException) {
                cause = cause.getCause();
            } else {
                break;
            }
        }

        if (cause instanceof StatusRuntimeException) {
            Status.Code code = ((StatusRuntimeException) cause).getStatus()
                    .getCode();
            return code == Status.Code.UNAUTHENTICATED;
        }

        if (cause instanceof ApiRuntimeException) {
            String msg = cause.getMessage();
            return msg != null && (msg.contains("UNAUTHENTICATED") || msg.contains("30052"));
        }

        return false;
    }

}
