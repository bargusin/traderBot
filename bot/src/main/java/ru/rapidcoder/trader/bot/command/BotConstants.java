package ru.rapidcoder.trader.bot.command;

public final class BotConstants {

    private BotConstants() {
    }

    public static final String CMD_START = "/start";
    public static final String CMD_HELP = "/help";
    public static final String CMD_CANCEL = "/cancel";
    public static final String CMD_ACCOUNT = "/account";
    public static final String CMD_PORTFOLIO = "/portfolio";
    public static final String CMD_SETTINGS = "/settings";
    public static final String CMD_PRICE = "/price";
    public static final String CMD_CHART = "/chart";

    public static final String CALLBACK_BACK_TO_MAIN = "back_to_main";
    public static final String CALLBACK_SETTINGS = CMD_SETTINGS;
    public static final String CALLBACK_CHANGE_SANDBOX_TOKEN = "change_sandbox_token";
    public static final String CALLBACK_CHANGE_PRODUCTION_TOKEN = "change_production_token";
    public static final String CALLBACK_CHART_PERIOD = "chart_period";

    public static final String BUTTON_BACK_TO_MAIN = "\uD83C\uDFE0 Главное меню";
    public static final String BUTTON_SETTINGS = "⚙\uFE0F Настройки";
    public static final String BUTTON_PORTFOLIO = "\uD83D\uDCBC Портфель";
    public static final String BUTTON_HELP = "\uD83D\uDCAC Помощь";
    public static final String BUTTON_ACCOUNTS = "\uD83D\uDCB0 Счета";
}
