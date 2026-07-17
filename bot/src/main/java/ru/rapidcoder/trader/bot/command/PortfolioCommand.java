package ru.rapidcoder.trader.bot.command;

import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.rapidcoder.trader.bot.Bot;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.models.Money;
import ru.tinkoff.piapi.core.models.Portfolio;
import ru.tinkoff.piapi.core.models.Position;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PortfolioCommand extends AbstractCommand {

    public PortfolioCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
    }

    @Override
    public void execute(Update update) {
        Long chatId = getChatId(update);

        String text = buildHeaderText(update, "\uD83D\uDCBC <b>Управление портфелем</b>");

        InlineKeyboardMarkup keyboard = backToMainKeyboard();

        InvestApi investApi = bot.getTradingSessionManager()
                .getApi(chatId);
        String accountId = bot.getTradingSessionManager()
                .getCurrentAccountId(chatId);

        if (StringUtils.isEmpty(accountId)) {
            processMessage(update, text + "\n\n\uD83D\uDEAB Счет не определен /account", keyboard);
        } else {
            investApi.getOperationsService()
                    .getPortfolio(accountId)
                    .thenCompose(portfolio -> {
                        return buildReportTextAsync(investApi, portfolio);
                    })
                    .thenAccept(reportText -> {
                        processMessage(update, text + reportText, keyboard);
                    })
                    .exceptionally(e -> {
                        processMessage(update, text + "\n\n\uD83D\uDEAB Ошибка при получении портфеля: " + e.getMessage(), keyboard);
                        return null;
                    });
        }
    }

    private CompletableFuture<String> buildReportTextAsync(InvestApi api, Portfolio portfolio) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n\n💰 Стоимость: ")
                .append(formatMoney(portfolio.getTotalAmountPortfolio()))
                .append("\n");
        String currency = portfolio.getTotalAmountPortfolio()
                .getCurrency();
        sb.append("📊 Доходность: ")
                .append(formatYield(portfolio.getExpectedYield(), currency))
                .append("\n");
        sb.append("─────────────────────\n");

        List<Position> positions = portfolio.getPositions();

        if (positions.isEmpty()) {
            sb.append("Позиций нет. Портфель пуст 🕸");
            return CompletableFuture.completedFuture(sb.toString());
        }

        List<CompletableFuture<String>> lineFutures = positions.stream()
                .map(pos -> formatPositionLineAsync(api, pos))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(lineFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    lineFutures.stream()
                            .map(CompletableFuture::join)
                            .forEach(sb::append);
                    return sb.toString();
                });
    }

    private CompletableFuture<String> formatPositionLineAsync(InvestApi api, Position pos) {
        return api.getInstrumentsService()
                .getInstrumentByFigi(pos.getFigi())
                .handle((instrument, ex) -> {
                    String name = (ex == null && instrument != null) ? instrument.getName() : "Неизвестный";
                    String ticker = (ex == null && instrument != null) ? instrument.getTicker() : pos.getFigi();

                    // В Core моделях getQuantity() сразу возвращает BigDecimal!
                    BigDecimal quantity = pos.getQuantity();

                    // Цены тоже приходят в удобном классе Money
                    String price = formatMoney(pos.getCurrentPrice());
                    String yield = formatYield(pos.getExpectedYield(), pos.getCurrentPrice()
                            .getCurrency());

                    return String.format("🔹 **%s** %s\n   %s шт. | %s | P/L: %s\n\n", ticker, name, quantity.toPlainString(), price, yield);
                });
    }

    // --- Упрощенный форматтер для Core моделей ---

    private String formatMoney(Money money) {
        if (money == null)
            return "0.00";
        // Money.getValue() возвращает BigDecimal, currency - String
        return String.format("%.2f %s", money.getValue(), formatCurrency(money.getCurrency()));
    }

    private String formatYield(BigDecimal value, String currencyCode) {
        if (value == null)
            return "0.00";
        // Добавляем знак "+" для положительных чисел
        String sign = value.compareTo(BigDecimal.ZERO) > 0 ? "+" : "";
        return String.format("%s%.2f %s", sign, value, formatCurrency(currencyCode));
    }

    private String formatCurrency(String currencyCode) {
        if (currencyCode == null)
            return "";
        switch (currencyCode.toUpperCase()) {
            case "RUB":
                return "₽";
            case "USD":
                return "$";
            case "EUR":
                return "€";
            case "CNY":
                return "¥";
            default:
                return currencyCode.toUpperCase();
        }
    }
}
