package ru.rapidcoder.trader.core.database.entity;

import jakarta.persistence.*;
import ru.rapidcoder.trader.core.TradingMode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "created_at")
    private Instant createdAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserSetting> settings = new ArrayList<>();

    public User() {
    }

    public void addSetting(TradingMode mode, String token) {
        UserSetting setting = new UserSetting(this, mode, token);
        this.settings.add(setting);
    }

    public Optional<UserSetting> getSettingByMode(TradingMode mode) {
        return settings.stream()
                .filter(s -> s.getMode()
                        .equals(mode))
                .findFirst();
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<UserSetting> getSettings() {
        return settings;
    }
}
