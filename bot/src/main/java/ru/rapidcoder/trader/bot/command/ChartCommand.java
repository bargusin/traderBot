package ru.rapidcoder.trader.bot.command;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.component.InterfaceFactory;
import ru.rapidcoder.trader.core.service.MarketDataService;
import ru.tinkoff.piapi.contract.v1.CandleInterval;
import ru.tinkoff.piapi.contract.v1.HistoricCandle;
import ru.tinkoff.piapi.core.utils.DateUtils;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ChartCommand extends AbstractCommand {

    private static final ZoneId MOEX_ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(MOEX_ZONE);
    private static final String[] BAR_CHARS = {"\u2581", "\u2582", "\u2583", "\u2584", "\u2585", "\u2586", "\u2587", "\u2588"};
    private static final int MAX_CANDLES = 30;

    public ChartCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
    }

    @Override
    public void execute(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallback(update);
        } else {
            handleMessage(update);
        }
    }

    private void handleMessage(Update update) {
        String messageText = update.getMessage()
                .getText();
        String ticker = messageText.replace("/chart", "")
                .trim();

        if (ticker.isEmpty()) {
            processMessage(update, "\uD83D\uDEAB Использование: /chart &lt;ticker&gt;", backToMainKeyboard());
            return;
        }

        Long chatId = getChatId(update);
        MarketDataService marketDataService = new MarketDataService(bot.getTradingSessionManager()
                .getApi(chatId));

        CandleInterval interval = CandleInterval.CANDLE_INTERVAL_DAY;
        marketDataService.findInstrumentByTicker(ticker)
                .thenCompose(instrumentOpt -> {
                    if (instrumentOpt.isEmpty()) {
                        processMessage(update, "\uD83D\uDEAB Инструмент не найден: " + ticker, backToMainKeyboard());
                        return CompletableFuture.completedFuture(null);
                    }
                    String figi = instrumentOpt.get()
                            .getFigi();
                    String name = instrumentOpt.get()
                            .getName();
                    Instant to = Instant.now();
                    Instant from = to.minusSeconds(30L * 24 * 60 * 60);
                    return marketDataService.getCandles(figi, from, to, interval)
                            .thenApply(candles -> {
                                String text = formatChart(name, ticker, candles, interval);
                                processMessage(update, text, periodKeyboard(ticker));
                                return null;
                            });
                })
                .exceptionally(ex -> {
                    logger.error("Ошибка ChartCommand для chatId={}: {}", getChatId(update), ex.getMessage(), ex);
                    processMessage(update, "\uD83D\uDEAB Ошибка: " + ex.getMessage(), backToMainKeyboard());
                    return null;
                });
    }

    private void handleCallback(Update update) {
        String data = update.getCallbackQuery()
                .getData();
        String[] parts = data.split("#");
        if (parts.length < 3) {
            processMessage(update, "\uD83D\uDEAB Ошибка данных callback", backToMainKeyboard());
            return;
        }
        String ticker = parts[1];
        String intervalName = parts[2];

        Long chatId = getChatId(update);
        MarketDataService marketDataService = new MarketDataService(bot.getTradingSessionManager()
                .getApi(chatId));

        marketDataService.findInstrumentByTicker(ticker)
                .thenCompose(instrumentOpt -> {
                    if (instrumentOpt.isEmpty()) {
                        processMessage(update, "\uD83D\uDEAB Инструмент не найден", backToMainKeyboard());
                        return CompletableFuture.completedFuture(null);
                    }
                    String figi = instrumentOpt.get()
                            .getFigi();
                    String name = instrumentOpt.get()
                            .getName();
                    Instant to = Instant.now();
                    Instant from = to.minusSeconds(30L * 24 * 60 * 60);
                    CandleInterval interval = CandleInterval.valueOf(intervalName);
                    return marketDataService.getCandles(figi, from, to, interval)
                            .thenApply(candles -> {
                                String text = formatChart(name, ticker, candles, interval);
                                processMessage(update, text, periodKeyboard(ticker));
                                return null;
                            });
                })
                .exceptionally(ex -> {
                    logger.error("Ошибка ChartCommand для chatId={}: {}", getChatId(update), ex.getMessage(), ex);
                    processMessage(update, "\uD83D\uDEAB Ошибка: " + ex.getMessage(), backToMainKeyboard());
                    return null;
                });
    }

    private String formatChart(String name, String ticker, List<HistoricCandle> candles, CandleInterval interval) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uD83D\uDCC8 <b>").append(name).append("</b> (").append(ticker).append(")\n");
        sb.append("\uD83D\uDCC5 Период: ").append(periodLabel(interval)).append("\n\n");

        if (candles.isEmpty()) {
            sb.append("Нет данных за выбранный период");
            return sb.toString();
        }

        List<HistoricCandle> limited = candles.size() > MAX_CANDLES
                ? candles.subList(candles.size() - MAX_CANDLES, candles.size())
                : candles;

        BigDecimal min = limited.stream()
                .map(c -> MapperUtils.quotationToBigDecimal(c.getLow()))
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal max = limited.stream()
                .map(c -> MapperUtils.quotationToBigDecimal(c.getHigh()))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);

        BigDecimal range = max.subtract(min);
        if (range.compareTo(BigDecimal.ZERO) == 0) {
            range = BigDecimal.ONE;
        }

        for (HistoricCandle candle : limited) {
            BigDecimal open = MapperUtils.quotationToBigDecimal(candle.getOpen());
            BigDecimal close = MapperUtils.quotationToBigDecimal(candle.getClose());
            BigDecimal low = MapperUtils.quotationToBigDecimal(candle.getLow());

            int barIdx = low.subtract(min)
                    .multiply(BigDecimal.valueOf(7))
                    .divide(range, 0, RoundingMode.HALF_UP)
                    .intValue();
            barIdx = Math.max(0, Math.min(7, barIdx));

            boolean isUp = close.compareTo(open) >= 0;

            Instant candleTime = DateUtils.timestampToInstant(candle.getTime());
            sb.append(TIME_FMT.format(candleTime))
                    .append(" ")
                    .append(isUp ? "\uD83D\uDCC8" : "\uD83D\uDCC9")
                    .append(" ")
                    .append(BAR_CHARS[barIdx])
                    .append(" ")
                    .append(close.setScale(2, RoundingMode.HALF_UP).toPlainString())
                    .append("\n");
        }

        BigDecimal lastClose = MapperUtils.quotationToBigDecimal(limited.get(limited.size() - 1)
                .getClose());
        BigDecimal firstOpen = MapperUtils.quotationToBigDecimal(limited.get(0)
                .getOpen());
        BigDecimal changePct = lastClose.subtract(firstOpen)
                .divide(firstOpen, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        String sign = changePct.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";

        sb.append("\n\uD83D\uDCB1 Текущая: <b>").append(lastClose.setScale(2, RoundingMode.HALF_UP)).append("</b>\n");
        sb.append("\uD83D\uDCC8 Изменение: ").append(sign).append(changePct.setScale(2, RoundingMode.HALF_UP)).append("%");

        return sb.toString();
    }

    private InlineKeyboardMarkup periodKeyboard(String ticker) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(
                createPeriodButton("1ч", "CANDLE_INTERVAL_1_MIN", ticker),
                createPeriodButton("4ч", "CANDLE_INTERVAL_4_HOUR", ticker),
                createPeriodButton("1д", "CANDLE_INTERVAL_DAY", ticker),
                createPeriodButton("1н", "CANDLE_INTERVAL_WEEK", ticker)
        ));
        rows.add(List.of(InterfaceFactory.createButton(BotConstants.BUTTON_BACK_TO_MAIN, BotConstants.CALLBACK_BACK_TO_MAIN)));
        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private InlineKeyboardButton createPeriodButton(String label, String interval, String ticker) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(label);
        button.setCallbackData("chart_period#" + ticker + "#" + interval);
        return button;
    }

    private String periodLabel(CandleInterval interval) {
        return switch (interval) {
            case CANDLE_INTERVAL_1_MIN, CANDLE_INTERVAL_5_MIN, CANDLE_INTERVAL_15_MIN, CANDLE_INTERVAL_30_MIN, CANDLE_INTERVAL_HOUR -> "Внутридневной";
            case CANDLE_INTERVAL_4_HOUR -> "4 часа";
            case CANDLE_INTERVAL_DAY -> "День";
            case CANDLE_INTERVAL_WEEK, CANDLE_INTERVAL_MONTH -> "Неделя";
            default -> interval.name();
        };
    }
}
