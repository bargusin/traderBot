package ru.rapidcoder.trader.bot.handler;


import ru.rapidcoder.trader.bot.component.Component;

/**
 * Обработчик события компонента
 */
public class Handler {

    private final Component component;

    public Handler(Component component) {
        this.component = component;
    }

    /**
     * Запуск обработчика
     */
    public String execute() {
        return component.execute();
    }

    /**
     * Получение идентификатора события
     * @return идентификатор события
     */
    public String getCallbackData() {
        return component.getCallbackData();
    }

}
