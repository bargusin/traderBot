package ru.rapidcoder.trader.bot.handler;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface InputHandler {

    /**
     * Обрабатывает входящее сообщение от пользователя
     *
     * @param update апдейт с сообщением
     * @return true, если ввод обработан и ожидание завершено (можно сбрасывать состояние).
     * false, если мы все еще ждем продолжения ввода (многошаговый сценарий).
     */
    boolean handleInput(Update update);

}
