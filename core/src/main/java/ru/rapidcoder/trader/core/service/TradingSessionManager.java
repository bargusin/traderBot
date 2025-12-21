package ru.rapidcoder.trader.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.rapidcoder.trader.core.database.entity.User;
import ru.rapidcoder.trader.core.database.entity.UserSetting;
import ru.rapidcoder.trader.core.database.repository.UserRepository;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class TradingSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(TradingSessionManager.class);

    private final UserRepository userRepository;

    private final EncryptionService encryptionService;

    private final AccountService accountService;

    private final Map<Long, TradingUserSession> sessionCache = new ConcurrentHashMap<>();

    public TradingSessionManager(UserRepository userRepository, EncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.accountService = new AccountService(this);
    }

    public InvestApi getApi(Long chatId) {
        TradingUserSession session = getSession(chatId);
        return session.getApi();
    }

    public TradingMode getCurrentMode(Long chatId) {
        if (sessionCache.containsKey(chatId)) {
            return sessionCache.get(chatId)
                    .getMode();
        }
        return getSession(chatId).getMode();
    }

    public String getCurrentAccountId(Long chatId) {
        if (sessionCache.containsKey(chatId)) {
            return sessionCache.get(chatId)
                    .getAccountId();
        }
        return getSession(chatId).getAccountId();
    }

    public void switchMode(Long chatId, TradingMode newMode) {
        logger.info("Переключение режима для пользователя {} на {}", chatId, newMode);
        User user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));

        String encryptedToken = user.getSettingByMode(TradingMode.valueOf(newMode.getStorageKey()))
                .map(UserSetting::getEncryptedToken)
                .orElseThrow(() -> new IllegalArgumentException("Токен для режима " + newMode.getStorageKey() + " не установлен. Сначала добавьте токен."));

        String token = encryptionService.decrypt(encryptedToken);
        userRepository.updateTradingMode(chatId, newMode);
        InvestApi newApi = createApiInstance(token, newMode);
        sessionCache.put(chatId, new TradingUserSession(newApi, newMode, null));
    }

    public void switchAccountId(Long chatId, String newAccountId) {
        logger.info("Установка идентификатора счета {} для пользователя {}", newAccountId, chatId);
        userRepository.findByChatId(chatId)
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
        userRepository.updateAccountId(chatId, newAccountId);
        TradingUserSession currentSession = getSession(chatId);
        sessionCache.put(chatId, new TradingUserSession(currentSession.getApi(), currentSession.getMode(), newAccountId));
    }

    private TradingUserSession getSession(Long chatId) {
        return sessionCache.computeIfAbsent(chatId, this::createSession);
    }

    private TradingUserSession createSession(Long chatId) {
        logger.info("Создание сессии для {}", chatId);
        User user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new IllegalStateException("Пользователь не зарегистрирован"));
        TradingMode modeToRestore = user.getCurrentMode();
        if (modeToRestore == null) {
            modeToRestore = TradingMode.SANDBOX;
        }
        Optional<UserSetting> settingOpt = user.getSettingByMode(modeToRestore == TradingMode.READONLY ? TradingMode.PRODUCTION : modeToRestore);
        if (settingOpt.isEmpty()) {
            throw new IllegalStateException("Нет активного токена для режима " + modeToRestore);
        }
        String token = encryptionService.decrypt(settingOpt.get()
                .getEncryptedToken());
        InvestApi api = createApiInstance(token, modeToRestore);
        return new TradingUserSession(api, modeToRestore, user.getCurrentAccountId());
    }

    private InvestApi createApiInstance(String token, TradingMode mode) {
        if (mode == TradingMode.SANDBOX) {
            return InvestApi.createSandbox(token);
        } else if (mode == TradingMode.READONLY) {
            return InvestApi.createReadonly(token);
        } else {
            return InvestApi.create(token);
        }
    }

    public void removeSession(Long chatId) {
        sessionCache.remove(chatId);
    }

    public AccountService getAccountService() {
        return accountService;
    }
}
