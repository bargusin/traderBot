package ru.rapidcoder.trader.core.sevice.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.rapidcoder.trader.core.service.EncryptionService;

import javax.crypto.KeyGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class EncryptionServiceTest {
    private String validKeyBase64;

    @BeforeEach
    void setUpAll() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        byte[] key = keyGen.generateKey()
                .getEncoded();
        validKeyBase64 = Base64.getEncoder()
                .encodeToString(key);
    }

    @Test
    @DisplayName("Должен успешно шифровать и расшифровывать строку")
    void testEncryptAndDecryptSuccessfully() {
        EncryptionService service = new EncryptionService(validKeyBase64);
        String originalText = "MySecretToken123:555";

        String encrypted = service.encrypt(originalText);
        String decrypted = service.decrypt(encrypted);

        assertNotNull(encrypted);
        assertNotEquals(originalText, encrypted);
        assertEquals(originalText, decrypted);
    }

    @Test
    @DisplayName("Каждое шифрование должно давать разный результат (из-за уникального IV)")
    void testProduceDifferentCiphertextForSameInput() {
        EncryptionService service = new EncryptionService(validKeyBase64);
        String text = "StaticData";

        String enc1 = service.encrypt(text);
        String enc2 = service.encrypt(text);

        assertNotEquals(enc1, enc2, "GCM режим должен использовать случайный IV для каждой операции");
        assertEquals(service.decrypt(enc1), service.decrypt(enc2));
    }

    @Test
    @DisplayName("Должен выбрасывать ошибку при попытке создать сервис с коротким ключом")
    void testThrowExceptionOnInvalidKeyLength() {
        // Given (Ключ всего 16 байт / 128 бит вместо 256)
        String shortKey = Base64.getEncoder()
                .encodeToString(new byte[16]);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new EncryptionService(shortKey);
        });

        assertTrue(exception.getMessage()
                .contains("Неверная длина ключа"));
    }

    @Test
    @DisplayName("Должен выбрасывать ошибку при попытке создать сервис с 'мусорным' ключом")
    void testThrowExceptionOnNotBase64Key() {
        String garbageKey = "NotABase64Key!!";

        assertThrows(IllegalArgumentException.class, () -> {
            new EncryptionService(garbageKey);
        });
    }

    @Test
    @DisplayName("Расшифровка должна падать, если данные повреждены (проверка целостности GCM)")
    void testFailDecryptionIfDataTampered() {
        EncryptionService service = new EncryptionService(validKeyBase64);
        String original = "BankPassword";
        String encryptedBase64 = service.encrypt(original);

        // Декодируем, портим один бит и кодируем обратно
        byte[] encryptedBytes = Base64.getDecoder()
                .decode(encryptedBase64);
        encryptedBytes[encryptedBytes.length - 1] ^= 1; // Инвертируем последний бит
        String tamperedBase64 = Base64.getEncoder()
                .encodeToString(encryptedBytes);

        // GCM выбросит AEADBadTagException (обернутый в RuntimeException в нашем сервисе)
        assertThrows(RuntimeException.class, () -> {
            service.decrypt(tamperedBase64);
        });
    }
}
