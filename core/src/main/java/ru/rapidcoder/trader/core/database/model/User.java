package ru.rapidcoder.trader.core.database.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", unique = true, nullable = false)
    private Long chatId;

    @Column(name = "encrypted_token")
    private String encryptedToken;

    @Column(name = "mode")
    private String mode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Геттеры, сеттеры, конструкторы
}
