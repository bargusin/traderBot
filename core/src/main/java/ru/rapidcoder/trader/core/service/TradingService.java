package ru.rapidcoder.trader.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.piapi.contract.v1.*;
import ru.tinkoff.piapi.core.InstrumentsService;
import ru.tinkoff.piapi.core.InvestApi;
import ru.tinkoff.piapi.core.OrdersService;
import ru.tinkoff.piapi.core.SandboxService;
import ru.tinkoff.piapi.core.models.Money;
import ru.tinkoff.piapi.core.models.Portfolio;
import ru.tinkoff.piapi.core.models.Position;

import java.util.List;
import java.util.UUID;

public class TradingService {

    private static final Logger logger = LoggerFactory.getLogger(TradingService.class);
    // Токен именно для SANDBOX (берется в настройках на сайте tinkoff.ru/invest)
    private static final String SANDBOX_TOKEN = "t.mfCfVNbET75vydgOJf7JpOWxJFuolgjqDKXkwXsbhZeVB998n5uYgWItZ26R_16skDj71T8blqyO9vo9IhL0Sg";
    private static TradingService instance;

    private TradingService() {
    }

    public static synchronized TradingService getTradingService() {
        if (instance == null) {
            instance = new TradingService();
        }
        return instance;
    }

    public static void main(String[] args) {
        // 1. Создаем подключение именно к песочнице (createSandbox)
        InvestApi api = InvestApi.createSandbox(SANDBOX_TOKEN);

        // Получаем сервисы
        SandboxService sandboxService = api.getSandboxService();
        InstrumentsService instrumentsService = api.getInstrumentsService();
        OrdersService ordersService = api.getOrdersService();

        try {
            // 2. Открываем новый счет в песочнице
            // В песочнице может быть несколько счетов, создаем новый для чистоты эксперимента
            String accountId = sandboxService.openAccountSync();
            System.out.println("Открыт новый счет в песочнице: " + accountId);

            // 3. Пополняем счет "виртуальными" деньгами
            // Например, кладем 100 000 рублей
            MoneyValue payInAmount = MoneyValue.newBuilder()
                    .setCurrency("rub")
                    .setUnits(100_000)
                    .setNano(0)
                    .build();

            sandboxService.payIn(accountId, payInAmount);
            System.out.println("Счет пополнен на 100 000 руб.");

            // 4. Ищем инструмент для покупки (например, акции Сбера по тикеру SBER)
            String ticker = "GAZP"; // Лучше использовать тикер Т-Банка или GAZP, с ними меньше проблем в песочнице
            List<InstrumentShort> foundInstruments = instrumentsService.findInstrumentSync(ticker);

            String figi = foundInstruments.stream()
                    // 1. Совпадение по тикеру
                    .filter(item -> item.getTicker()
                            .equals(ticker))
                    // 2. Тип инструмента - Акция ("share")
                    .filter(item -> item.getInstrumentType()
                            .equals("share"))
                    // 3. ВАЖНО: Проверяем, доступна ли торговля через API
                    .filter(item -> item.getApiTradeAvailableFlag())
                    // 4. (Опционально) Фильтр по площадке, обычно TQBR - это основной режим торгов Мосбиржи
                    // .filter(item -> "TQBR".equals(item.getClassCode()))
                    .findFirst()
                    .map(InstrumentShort::getFigi)
                    .orElseThrow(() -> new RuntimeException("Инструмент " + ticker + " доступный для торговли через API не найден"));

            System.out.println("Выбран FIGI: " + figi);

            // Дополнительная проверка: получаем полную информацию об инструменте
            Instrument instrument = instrumentsService.getInstrumentByFigiSync(figi);
            if (!instrument.getBuyAvailableFlag()) {
                throw new RuntimeException("Покупка данного инструмента сейчас недоступна (рынок закрыт или ограничение)");
            }

            // 5. Выставляем рыночную заявку на покупку (Market Order)
            // Покупаем 1 лот
            int quantity = 1;
            String orderId = UUID.randomUUID()
                    .toString(); // Уникальный ID заявки (генерируем сами)

            PostOrderResponse orderResponse = ordersService.postOrderSync(figi, quantity, Quotation.getDefaultInstance(), // Для рыночной заявки цена не важна
                    OrderDirection.ORDER_DIRECTION_BUY, accountId, OrderType.ORDER_TYPE_MARKET, orderId);

            System.out.println("Заявка исполнена. Статус: " + orderResponse.getExecutionReportStatus());
            System.out.println("Куплено лотов: " + orderResponse.getLotsExecuted());

            // 6. Проверяем портфель
            Portfolio portfolio = Portfolio.fromResponse(sandboxService.getPortfolioSync(accountId));
            System.out.println("=== Текущий портфель ===");

            // Выводим рубли
            Money totalAmount = portfolio.getTotalAmountCurrencies();
            System.out.println("Остаток валюты: " + totalAmount.getValue() + " " + totalAmount.getCurrency());

            // Выводим позиции
            List<Position> positions = portfolio.getPositions();
            for (Position position : positions) {
                System.out.println("Инструмент (FIGI): " + position.getFigi() + " | Количество: " + position.getQuantity());
            }

            // 7. (Опционально) Закрываем счет в конце работы
            List<Account> accounts = sandboxService.getAccounts()
                    .get();
            for (Account account : accounts) {
                //sandboxService.closeAccountSync(account.getId());
                System.out.println("Счет " + account.getId() + " закрыт.");
            }

        } catch (Exception e) {
            System.err.println("Произошла ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
