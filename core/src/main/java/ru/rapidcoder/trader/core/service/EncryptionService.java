package ru.rapidcoder.trader.core.service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128; // Длина тега аутентификации
    private static final int IV_LENGTH_BYTE = 12;  // Рекомендуемая длина IV для GCM (12 байт)

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public EncryptionService(String keyBase64) {
        this.secureRandom = new SecureRandom();

        if (keyBase64 == null || keyBase64.isBlank()) {
            throw new IllegalStateException("Переменная окружения ENCRYPTION_KEY не установлена!");
        }

        // Декодируем ключ из Base64
        byte[] decodedKey = Base64.getDecoder()
                .decode(keyBase64);

        // Валидация длины ключа (для AES-256 должно быть 32 байта)
        if (decodedKey.length != 32) {
            throw new IllegalArgumentException("Неверная длина ключа! Ожидается 256 бит (32 байта) в Base64.");
        }

        this.secretKey = new SecretKeySpec(decodedKey, "AES");
    }

    /**
     * Шифрует строку.
     * Возвращает Base64 строку формата: [IV (12 байт)] + [Ciphertext]
     */
    public String encrypt(String rawData) {
        try {
            // 1. Генерируем уникальный IV для этой операции
            byte[] iv = new byte[IV_LENGTH_BYTE];
            secureRandom.nextBytes(iv);

            // 2. Настраиваем шифр
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // 3. Шифруем данные
            byte[] cipherText = cipher.doFinal(rawData.getBytes(StandardCharsets.UTF_8));

            // 4. Объединяем IV и CipherText в один массив
            // Это нужно, чтобы сохранить IV вместе с данными (он нужен для расшифровки)
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);
            byte[] combined = byteBuffer.array();

            // 5. Возвращаем результат в Base64
            return Base64.getEncoder()
                    .encodeToString(combined);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при шифровании данных", e);
        }
    }

    /**
     * Расшифровывает строку.
     * Ожидает Base64 строку формата: [IV (12 байт)] + [Ciphertext]
     */
    public String decrypt(String encryptedData) {
        try {
            // 1. Декодируем из Base64
            byte[] decoded = Base64.getDecoder()
                    .decode(encryptedData);

            // 2. Разделяем на IV и CipherText
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[IV_LENGTH_BYTE];
            byteBuffer.get(iv); // Читаем первые 12 байт

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText); // Читаем остальное

            // 3. Настраиваем шифр для расшифровки
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // 4. Расшифровываем
            byte[] plainText = cipher.doFinal(cipherText);

            return new String(plainText, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при расшифровке данных (возможно, неверный ключ или поврежденные данные)", e);
        }
    }
}
