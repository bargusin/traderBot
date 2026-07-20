package ru.rapidcoder.trader.bot.command.trading;

import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.rapidcoder.trader.bot.Bot;
import ru.rapidcoder.trader.bot.command.AbstractCommand;
import ru.rapidcoder.trader.bot.command.BotConstants;
import ru.rapidcoder.trader.bot.component.InterfaceFactory;
import ru.rapidcoder.trader.bot.handler.InputHandler;
import ru.rapidcoder.trader.core.service.MarketDataService;
import ru.rapidcoder.trader.core.service.TradingService;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.models.Portfolio;
import ru.tinkoff.piapi.core.models.Position;
import ru.tinkoff.piapi.core.utils.MapperUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SellCommand extends AbstractCommand {

    public SellCommand(Bot bot, String identifier, String description) {
        super(bot, identifier, description);
    }

    @Override
    public void execute(Update update) {
        Long chatId = getChatId(update);
        String messageText = update.hasMessage() ? update.getMessage().getText() : "";
        String ticker = messageText.replace(BotConstants.CMD_SELL, "").trim();

        if (!ticker.isEmpty()) {
            processInstrumentLookup(update, chatId, ticker);
        } else {
            processMessage(update, "\uD83D\uDD0D Введите тикер инструмента для продажи:", cancelKeyboard());
            userStateService.setInputHandler(chatId, new TickerInputHandler(update, chatId));
        }
    }

    private void processInstrumentLookup(Update update, Long chatId, String ticker) {
        InvestApi investApi = bot.getTradingSessionManager().getApi(chatId);
        String accountId = bot.getTradingSessionManager().getCurrentAccountId(chatId);

        if (StringUtils.isEmpty(accountId)) {
            processMessage(update, "\uD83D\uDEAB Счет не определен. Используйте /account", backToMainKeyboard());
            return;
        }

        MarketDataService marketDataService = new MarketDataService(investApi);

        String headerText = buildHeaderText(update, "\uD83D\uDED2 <b>Продажа</b>");

        marketDataService.findInstrumentByTicker(ticker)
                .thenCompose(instrumentOpt -> {
                    if (instrumentOpt.isEmpty()) {
                        processMessage(update, headerText + "\n\n\uD83D\uDEAB Инструмент не найден: " + ticker, backToMainKeyboard());
                        return CompletableFuture.completedFuture(null);
                    }
                    InstrumentShort instrument = instrumentOpt.get();

                    return investApi.getOperationsService()
                            .getPortfolio(accountId)
                            .thenCombine(marketDataService.getLastPrices(instrument.getFigi()), (portfolio, prices) -> {
                                long positionQuantity = findPositionQuantity(portfolio, instrument.getFigi());
                                BigDecimal currentPrice = prices.isEmpty()
                                        ? BigDecimal.ZERO
                                        : MapperUtils.quotationToBigDecimal(prices.get(0).getPrice());

                                if (positionQuantity == 0) {
                                    processMessage(update, headerText + "\n\n\uD83D\uDEAB У вас нет позиции по " + instrument.getTicker(), backToMainKeyboard());
                                    return null;
                                }

                                String text = headerText + "\n\n"
                                        + "\uD83D\uDCB1 <b>" + instrument.getName() + "</b> (" + instrument.getTicker() + ")\n"
                                        + "\uD83D\uDCB1 Текущая цена: " + currentPrice.setScale(2, RoundingMode.HALF_UP) + "\n"
                                        + "\uD83D\uDCCA Ваша позиция: " + positionQuantity + " лот(ов)\n\n"
                                        + "\uD83D\uDD0D Введите количество лотов для продажи:";

                                processMessage(update, text, cancelKeyboard());
                                userStateService.setInputHandler(chatId,
                                        new QuantityInputHandler(update, chatId, investApi, accountId, instrument, positionQuantity, currentPrice));
                                return null;
                            });
                })
                .exceptionally(ex -> {
                    logger.error("Ошибка SellCommand для chatId={}: {}", chatId, ex.getMessage(), ex);
                    processMessage(update, headerText + "\n\n\uD83D\uDEAB Ошибка: " + ex.getMessage(), backToMainKeyboard());
                    return null;
                });
    }

    private long findPositionQuantity(Portfolio portfolio, String figi) {
        for (Position pos : portfolio.getPositions()) {
            if (figi.equals(pos.getFigi())) {
                return pos.getQuantity().longValue();
            }
        }
        return 0;
    }

    private class TickerInputHandler implements InputHandler {

        private final Update originalUpdate;
        private final Long chatId;

        TickerInputHandler(Update originalUpdate, Long chatId) {
            this.originalUpdate = originalUpdate;
            this.chatId = chatId;
        }

        @Override
        public boolean handleInput(Update update) {
            String ticker = update.getMessage().getText().trim();
            processInstrumentLookup(originalUpdate, chatId, ticker);
            return true;
        }
    }

    private class QuantityInputHandler implements InputHandler {

        private final Update originalUpdate;
        private final Long chatId;
        private final InvestApi investApi;
        private final String accountId;
        private final InstrumentShort instrument;
        private final long positionQuantity;
        private final BigDecimal currentPrice;

        QuantityInputHandler(Update originalUpdate, Long chatId, InvestApi investApi, String accountId,
                             InstrumentShort instrument, long positionQuantity, BigDecimal currentPrice) {
            this.originalUpdate = originalUpdate;
            this.chatId = chatId;
            this.investApi = investApi;
            this.accountId = accountId;
            this.instrument = instrument;
            this.positionQuantity = positionQuantity;
            this.currentPrice = currentPrice;
        }

        @Override
        public boolean handleInput(Update update) {
            String input = update.getMessage().getText().trim();
            long lots;
            try {
                lots = Long.parseLong(input);
            } catch (NumberFormatException e) {
                processMessage(update, "\uD83D\uDEAB Введите целое число:", cancelKeyboard());
                return false;
            }

            if (lots <= 0) {
                processMessage(update, "\uD83D\uDEAB Количество должно быть больше 0:", cancelKeyboard());
                return false;
            }

            if (lots > positionQuantity) {
                processMessage(update, "\uD83D\uDEAB Недостаточно позиций. У вас: " + positionQuantity + " лот(ов)", cancelKeyboard());
                return false;
            }

            BigDecimal totalProceeds = currentPrice.multiply(BigDecimal.valueOf(lots));
            String text = buildHeaderText(originalUpdate, "\uD83D\uDED2 <b>Подтверждение продажи</b>") + "\n\n"
                    + "\uD83D\uDCB1 <b>" + instrument.getName() + "</b> (" + instrument.getTicker() + ")\n"
                    + "\uD83D\uDCB1 Цена: " + currentPrice.setScale(2, RoundingMode.HALF_UP) + "\n"
                    + "\uD83D\uDCCA Количество: " + lots + " лот(ов)\n"
                    + "\uD83D\uDCB0 Итого: ~" + totalProceeds.setScale(2, RoundingMode.HALF_UP) + "\n\n"
                    + "\u2757 Подтвердите продажу:";

            InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
            keyboard.setKeyboard(List.of(
                    List.of(
                            InterfaceFactory.createButton("\u2705 Продать", BotConstants.CALLBACK_CONFIRM_ORDER + "#SELL_" + instrument.getTicker() + "_" + lots),
                            InterfaceFactory.createButton("\u274C Отмена", BotConstants.CMD_CANCEL)
                    )
            ));

            processMessage(originalUpdate, text, keyboard);
            return true;
        }
    }
}
