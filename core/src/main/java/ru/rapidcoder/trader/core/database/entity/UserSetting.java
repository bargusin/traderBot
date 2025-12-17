package ru.rapidcoder.trader.core.database.entity;

import jakarta.persistence.*;
import ru.rapidcoder.trader.core.service.TradingMode;

@Entity
@Table(name = "user_settings")
public class UserSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false)
    private TradingMode mode;

    @Column(name = "encrypted_token", nullable = false)
    private String encryptedToken;

    public UserSetting() {
    }

    public UserSetting(User user, TradingMode mode, String encryptedToken) {
        this.user = user;
        this.mode = mode;
        this.encryptedToken = encryptedToken;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public TradingMode getMode() {
        return mode;
    }

    public void setMode(TradingMode mode) {
        this.mode = mode;
    }

    public String getEncryptedToken() {
        return encryptedToken;
    }

    public void setEncryptedToken(String encryptedToken) {
        this.encryptedToken = encryptedToken;
    }
}
