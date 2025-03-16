package ru.rapidcoder.trader.bot.handler;

import java.util.HashMap;
import java.util.Map;

/**
 * Обработчик событий
 */
public class HandlerExcecutor {

    private final Map<String, Handler> handlers;

    private static final HandlerExcecutor handlerExcecutor = new HandlerExcecutor();

    private HandlerExcecutor() {
        handlers = new HashMap<>();
    }

    public static HandlerExcecutor getHandlerExcecutor() {
        return handlerExcecutor;
    }

    /**
     * Запуск обработчика события
     * @param callbackData идентификатор события
     * @return результат ответа события
     */
    public String execute(String callbackData) {
        if (handlers.containsKey(callbackData)) {
            Handler handler = handlers.get(callbackData);
            return handler.execute();
        } else {
            throw new RuntimeException("Handler by callbackData='" + callbackData + "' not found");
        }
    }

    /**
     * Добавление нового обрабочика событий
     *
     * @param handler обработчик события
     */
    public void add(Handler handler) {
        handlers.put(handler.getCallbackData(), handler);
    }

}
