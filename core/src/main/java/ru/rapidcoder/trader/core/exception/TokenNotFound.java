package ru.rapidcoder.trader.core.exception;

public class TokenNotFound extends IllegalStateException {

    public TokenNotFound(String message) {
        super(message);
    }
}
