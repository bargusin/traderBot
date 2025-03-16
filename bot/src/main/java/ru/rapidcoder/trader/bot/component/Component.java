package ru.rapidcoder.trader.bot.component;

public interface Component {

    /**
     * Получение идентификатора события компонента
     * @return идентификатор события компонента
     */
    String getCallbackData();

    /**
     * Запуск обработки события
     * @return ответ на событие компонента
     */
    String execute();

}
