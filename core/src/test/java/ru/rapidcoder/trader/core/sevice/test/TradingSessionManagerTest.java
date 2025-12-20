package ru.rapidcoder.trader.core.sevice.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.rapidcoder.trader.core.database.entity.User;
import ru.rapidcoder.trader.core.database.entity.UserSetting;
import ru.rapidcoder.trader.core.database.repository.UserRepository;
import ru.rapidcoder.trader.core.service.EncryptionService;
import ru.rapidcoder.trader.core.service.TradingMode;
import ru.rapidcoder.trader.core.service.TradingSessionManager;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TradingSessionManagerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private TradingSessionManager sessionManager;

    private MockedStatic<InvestApi> investApiMockedStatic;

    @Mock
    private InvestApi investApiMock;

    @BeforeEach
    void setUp() {
        investApiMockedStatic = mockStatic(InvestApi.class);
    }

    @AfterEach
    void tearDown() {
        investApiMockedStatic.close();
    }

    @Test
    @DisplayName("getApi: Должен создать новую сессию (SANDBOX), если её нет в кеше")
    void testGetApiCreatesNewSession() {
        Long chatId = 123L;
        String encryptedToken = "enc_token";
        String decryptedToken = "dec_token";

        User user = mock(User.class);
        UserSetting setting = mock(UserSetting.class);

        when(userRepository.findByChatId(chatId)).thenReturn(Optional.of(user));

        when(user.getSettingByMode(TradingMode.SANDBOX)).thenReturn(Optional.of(setting));
        when(setting.getEncryptedToken()).thenReturn(encryptedToken);

        when(encryptionService.decrypt(encryptedToken)).thenReturn(decryptedToken);

        investApiMockedStatic.when(() -> InvestApi.createSandbox(decryptedToken))
                .thenReturn(investApiMock);

        InvestApi result = sessionManager.getApi(chatId);

        assertNotNull(result);
        assertEquals(investApiMock, result);

        investApiMockedStatic.verify(() -> InvestApi.createSandbox(decryptedToken));
        verify(userRepository, times(1)).findByChatId(chatId);
    }

    @Test
    @DisplayName("getApi: Должен возвращать кешированную сессию и не ходить в БД повторно")
    void testGetApiReturnCachedSession() {
        Long chatId = 123L;

        User user = mock(User.class);
        UserSetting setting = mock(UserSetting.class);
        when(userRepository.findByChatId(chatId)).thenReturn(Optional.of(user));
        when(user.getSettingByMode(TradingMode.SANDBOX)).thenReturn(Optional.of(setting));
        when(setting.getEncryptedToken()).thenReturn("enc");
        when(encryptionService.decrypt("enc")).thenReturn("dec");
        investApiMockedStatic.when(() -> InvestApi.createSandbox("dec"))
                .thenReturn(investApiMock);

        sessionManager.getApi(chatId);

        InvestApi result2 = sessionManager.getApi(chatId);

        assertNotNull(result2);

        verify(userRepository, times(1)).findByChatId(chatId);
        verify(encryptionService, times(1)).decrypt(anyString());
    }

    @Test
    @DisplayName("switchMode: Должен переключить режим на PRODUCTION и обновить кеш")
    void testSwitchModeSuccess() {
        Long chatId = 123L;
        TradingMode newMode = TradingMode.PRODUCTION;

        User user = mock(User.class);
        UserSetting setting = mock(UserSetting.class);

        when(userRepository.findByChatId(chatId)).thenReturn(Optional.of(user));

        when(user.getSettingByMode(TradingMode.PRODUCTION)).thenReturn(Optional.of(setting));

        when(setting.getEncryptedToken()).thenReturn("enc_prod");
        when(encryptionService.decrypt("enc_prod")).thenReturn("dec_prod");

        investApiMockedStatic.when(() -> InvestApi.create("dec_prod"))
                .thenReturn(investApiMock);

        sessionManager.switchMode(chatId, newMode);

        TradingMode currentMode = sessionManager.getCurrentMode(chatId);
        assertEquals(TradingMode.PRODUCTION, currentMode);

        investApiMockedStatic.verify(() -> InvestApi.create("dec_prod"));
    }

    @Test
    @DisplayName("switchMode: Должен переключить режим на READONLY и обновить кеш")
    void testSwitchReadonlyModeSuccess() {
        Long chatId = 123L;
        TradingMode newMode = TradingMode.READONLY;

        User user = mock(User.class);
        UserSetting setting = mock(UserSetting.class);

        when(userRepository.findByChatId(chatId)).thenReturn(Optional.of(user));

        when(user.getSettingByMode(TradingMode.PRODUCTION)).thenReturn(Optional.of(setting));

        when(setting.getEncryptedToken()).thenReturn("enc_prod");
        when(encryptionService.decrypt("enc_prod")).thenReturn("dec_prod");

        investApiMockedStatic.when(() -> InvestApi.createReadonly("dec_prod"))
                .thenReturn(investApiMock);

        sessionManager.switchMode(chatId, newMode);

        TradingMode currentMode = sessionManager.getCurrentMode(chatId);
        assertEquals(TradingMode.READONLY, currentMode);

        investApiMockedStatic.verify(() -> InvestApi.createReadonly("dec_prod"));
    }

    @Test
    @DisplayName("switchMode: Должен выбросить IllegalArgumentException, если нет токена")
    void testSwitchModeNoToken() {
        Long chatId = 123L;
        TradingMode newMode = TradingMode.PRODUCTION;

        User user = mock(User.class);
        when(userRepository.findByChatId(chatId)).thenReturn(Optional.of(user));

        when(user.getSettingByMode(TradingMode.PRODUCTION)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            sessionManager.switchMode(chatId, newMode);
        });

        verifyNoInteractions(encryptionService);
    }

    @Test
    @DisplayName("getCurrentMode: Должен возвращать SANDBOX по умолчанию, если сессии нет")
    void testGetCurrentModeDefault() {
        assertEquals(TradingMode.SANDBOX, sessionManager.getCurrentMode(999L));
    }

    @Test
    @DisplayName("createSession: Должен выбросить IllegalStateException, если у пользователя нет токена SANDBOX")
    void testCreateSessionNoSandboxToken() {
        Long chatId = 444L;
        User user = mock(User.class);

        when(userRepository.findByChatId(chatId)).thenReturn(Optional.of(user));
        when(user.getSettingByMode(TradingMode.SANDBOX)).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            sessionManager.getApi(chatId);
        });

        assertTrue(ex.getMessage()
                .contains("Нет активного токена"));
    }

    @Test
    @DisplayName("removeSession: Должен удалять сессию из кеша")
    void testRemoveSession() {
        Long chatId = 123L;

        User user = mock(User.class);
        UserSetting setting = mock(UserSetting.class);
        when(userRepository.findByChatId(chatId)).thenReturn(Optional.of(user));
        when(user.getSettingByMode(TradingMode.SANDBOX)).thenReturn(Optional.of(setting));
        when(setting.getEncryptedToken()).thenReturn("t");
        when(encryptionService.decrypt("t")).thenReturn("t");
        investApiMockedStatic.when(() -> InvestApi.createSandbox("t"))
                .thenReturn(investApiMock);

        sessionManager.getApi(chatId);

        sessionManager.removeSession(chatId);

        sessionManager.getApi(chatId);

        verify(userRepository, times(2)).findByChatId(chatId);
    }
}
