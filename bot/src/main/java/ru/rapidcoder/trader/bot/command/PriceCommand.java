package ru.rapidcoder.trader.bot.command;

import org.telegram.telegrambots.meta.api.objects.Update;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.core.service.MarketDataService;
import ru.tinkoff.piapi.contract.v1.InstrumentShort;
import ru.tinkoff.piapi.contract.v1.LastPrice;
import ru.tinkoff.piapi.contract.v1.SecurityTradingStatus;
import ru.tinkoff.piapi.core.utils.DateUtils;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PriceCommand extends AbstractCommand {

    private static final ZoneId MOEX_ZONE = ZoneId.of("Europe/Moscow");

    public PriceCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
    }

    @Override
    public void execute(Update update) {
        String messageText = update.getMessage()
                .getText();
        String ticker = messageText.replace("/price", "")
                .trim();

        if (ticker.isEmpty()) {
            processMessage(update, "\uD83D\uDEAB Использование: /price &lt;ticker&gt;", backToMainKeyboard());
            return;
        }

        Long chatId = getChatId(update);

        MarketDataService marketDataService = new MarketDataService(bot.getTradingSessionManager()
                .getApi(chatId));

        marketDataService.findInstrumentByTicker(ticker)
                .thenCompose(instrumentOpt -> {
                    if (instrumentOpt.isEmpty()) {
                        processMessage(update, "\uD83D\uDEAB Инструмент не найден: " + ticker, backToMainKeyboard());
                        return java.util.concurrent.CompletableFuture.completedFuture(null);
                    }
                    InstrumentShort instrument = instrumentOpt.get();
                    return marketDataService.getLastPrices(instrument.getFigi())
                            .thenCombine(marketDataService.getTradingStatus(instrument.getFigi()), (prices, status) -> {
                                String text = formatPriceResponse(instrument, prices, status);
                                processMessage(update, text, backToMainKeyboard());
                                return null;
                            });
                })
                .exceptionally(ex -> {
                    processMessage(update, "\uD83D\uDEAB Ошибка: " + ex.getMessage(), backToMainKeyboard());
                    return null;
                });
    }

    private String formatPriceResponse(InstrumentShort instrument, List<LastPrice> prices, ru.tinkoff.piapi.contract.v1.GetTradingStatusResponse status) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uD83D\uDCB1 <b>").append(instrument.getName()).append("</b> (").append(instrument.getTicker()).append(")\n\n");

        if (!prices.isEmpty()) {
            LastPrice lastPrice = prices.get(0);
            BigDecimal price = MapperUtils.quotationToBigDecimal(lastPrice.getPrice());
            sb.append("\uD83D\uDCB1 Цена: <b>").append(price.setScale(2, RoundingMode.HALF_UP).toPlainString()).append("</b>\n");

            Instant priceTime = DateUtils.timestampToInstant(lastPrice.getTime());
            if (priceTime != null) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(MOEX_ZONE);
                sb.append("\uD83D\uDD50 Время: ").append(fmt.format(priceTime)).append("\n");
            }
        }

        if (status != null) {
            SecurityTradingStatus tradingStatus = status.getTradingStatus();
            if (tradingStatus == SecurityTradingStatus.SECURITY_TRADING_STATUS_NORMAL_TRADING) {
                sb.append("\u2705 Статус: Торги открыты\n");
            } else if (tradingStatus == SecurityTradingStatus.SECURITY_TRADING_STATUS_NOT_AVAILABLE_FOR_TRADING) {
                sb.append("\u274C Статус: Торги закрыты\n");
            } else {
                sb.append("\u26A0\uFE0F Статус: ").append(tradingStatus.name()).append("\n");
            }
        }

        return sb.toString();
    }
}
