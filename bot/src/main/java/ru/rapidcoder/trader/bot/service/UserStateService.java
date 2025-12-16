package ru.rapidcoder.trader.bot.service;

import ru.rapidcoder.trader.bot.handler.InputHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserStateService {

    private static UserStateService instance;

    private final Map<Long, InputHandler> activeHandlers = new ConcurrentHashMap<>();

    private UserStateService() {

    }

    public static synchronized UserStateService getInstance() {
        if (instance == null) {
            instance = new UserStateService();
        }
        return instance;
    }

    public void setInputHandler(Long userId, InputHandler handler) {
        activeHandlers.put(userId, handler);
    }

    public InputHandler getInputHandler(Long userId) {
        return activeHandlers.get(userId);
    }

    public void clearState(Long userId) {
        activeHandlers.remove(userId);
    }
}
