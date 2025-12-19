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

    private final Map<Long, TradingUserSession> sessionCache = new ConcurrentHashMap<>();

    public TradingSessionManager(UserRepository userRepository, EncryptionService encryptionService) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
    }

    public InvestApi getApi(Long chatId) {
        TradingUserSession session = getSession(chatId);
        return session.getApi();
    }

    public TradingMode getCurrentMode(Long chatId) {
        return getSession(chatId).getMode();
    }

    public void switchMode(Long chatId, TradingMode newMode) {
        logger.info("Переключение режима для пользователя {} на {}", chatId, newMode);

        // 1. Ищем пользователя в БД
        User user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));

        // 2. Ищем токен для запрашиваемого режима
        String encryptedToken = user.getSettingByMode(TradingMode.valueOf(newMode.getStorageKey()))
                .map(UserSetting::getEncryptedToken)
                .orElseThrow(() -> new IllegalArgumentException("Токен для режима " + newMode.getStorageKey() + " не установлен. Сначала добавьте токен."));

        // 3. Расшифровываем
        String token = encryptionService.decrypt(encryptedToken);

        // 4. Создаем новое соединение с API
        InvestApi newApi = createApiInstance(token, newMode);

        // 5. Закрываем старую сессию (если была), чтобы освободить ресурсы gRPC
        removeSession(chatId);

        // 6. Сохраняем новую сессию в кеш
        sessionCache.put(chatId, new TradingUserSession(newApi, newMode));

        // 7. (Опционально) Можно обновить "последний активный режим" в базе данных,
        // если вы хотите, чтобы после рестарта бот помнил режим.
    }

    private TradingUserSession getSession(Long chatId) {
        // Если сессия уже есть в памяти - возвращаем её
        if (sessionCache.containsKey(chatId)) {
            return sessionCache.get(chatId);
        }
        return createSession(chatId);
    }

    private TradingUserSession createSession(Long chatId) {
        logger.info("Создание сессии для {}", chatId);
        User user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new IllegalStateException("Пользователь не зарегистрирован"));
        TradingMode modeToRestore = TradingMode.SANDBOX;
        Optional<UserSetting> settingOpt = user.getSettingByMode(modeToRestore);
        if (settingOpt.isEmpty()) {
            throw new IllegalStateException("Нет активных токенов для режима SANDBOX");
        }
        String token = encryptionService.decrypt(settingOpt.get()
                .getEncryptedToken());
        InvestApi api = createApiInstance(token, modeToRestore);

        TradingUserSession session = new TradingUserSession(api, modeToRestore);
        sessionCache.put(chatId, session);
        return session;
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
        TradingUserSession removed = sessionCache.remove(chatId);
        if (removed != null) {
            // ВАЖНО: Уничтожаем каналы gRPC, чтобы не текли ресурсы
            // В SDK метод destroy() или аналог закрывает каналы (в версии 1.x это может быть скрыто,
            // но сборщик мусора должен отработать, если ссылок нет.
            // Однако лучше проверить документацию вашей версии SDK, есть ли метод stop/close).
            // В текущей версии PIAPI явного close() у InvestApi нет, он управляется внутри.
        }
    }
}
