## 1. Сервис MarketDataService в core

- [x] 1.1 Создать класс `MarketDataService` в `core/src/main/java/ru/rapidcoder/trader/core/service/` — обёртка над InvestAPI с возвращаемыми типами CompletableFuture
- [x] 1.2 Реализовать in-memory кеш на ConcurrentHashMap + TTL (60с для цен, 300с для свечей, 600с для стакана, макс. 500 записей, FIFO eviction)
- [x] 1.3 Реализовать метод `getCandles(figi, from, to, interval)` — делегирование в InvestAPI `MarketDataService.getCandles`
- [x] 1.4 Реализовать метод `getLastPrices(figi)` — делегирование в InvestAPI `MarketDataService.getLastPrices`
- [x] 1.5 Реализовать метод `getOrderBook(figi, depth)` — делегирование в InvestAPI `MarketDataService.getOrderBook`
- [x] 1.6 Реализовать метод `getTradingStatus(figi)` — делегирование в InvestAPI `MarketDataService.getTradingStatus`
- [x] 1.7 Реализовать метод `findInstrumentByTicker(ticker)` через `InstrumentsService.findInstrument` — возврат первого совпадения
- [x] 1.8 Добавить обработку ошибок: перехват `ApiRuntimeException`, возврат `CompletableFuture.failedFuture()` с сообщением

## 2. Команда /price

- [x] 2.1 Создать класс `PriceCommand` в `bot/src/main/java/ru/rapidcoder/trader/bot/command/` — наследник `AbstractCommand`
- [x] 2.2 Реализовать `/price <ticker>` — валидация аргумента тикера, вызов `findInstrumentByTicker`, получение `getLastPrices` + `getTradingStatus`
- [x] 2.3 Форматирование ответа: название инструмента, тикер, текущая цена, дневное изменение, статус торгов, время последней цены
- [x] 2.4 Добавить клавиатуру «Главное меню» через хелпер `backToMainKeyboard()`
- [x] 2.5 Зарегистрировать `PriceCommand` в `MessageHandler` с идентификатором `/price`

## 3. Команда /chart

- [x] 3.1 Создать класс `ChartCommand` в `bot/src/main/java/ru/rapidcoder/trader/bot/command/` — наследник `AbstractCommand`
- [x] 3.2 Реализовать `/chart <ticker>` — валидация аргумента тикера, вызов `findInstrumentByTicker`, получение дневных свечей за 30 дней
- [x] 3.3 Реализовать форматирование текстового графика: свечи с метками времени, OHLC, шкала цен через символы ▁▂▃▄▅▆▇█
- [x] 3.4 Добавить inline-кнопки выбора периода: «1ч», «4ч», «1д», «1н» с callback data `chart_period#<ticker>#<interval>`
- [x] 3.5 Реализовать обработку callback для смены периода — редактирование существующего сообщения с новыми данными графика
- [x] 3.6 Зарегистрировать `ChartCommand` в `MessageHandler` с идентификатором `/chart`

## 4. Интеграция и регистрация

- [x] 4.1 Зарегистрировать `PriceCommand` и `ChartCommand` в конструкторе `MessageHandler`
- [x] 4.2 Добавить обработчик callback для смены периода в `MessageHandler.handleCallback` для префикса `chart_period`
- [x] 4.3 Проверить, что `BotConstants` содержит нужные константы callback

## 5. Тестирование

- [x] 5.1 Написать юнит-тесты для поведения кеша `MarketDataService` (истечение TTL, макс. размер, cache hit/miss)
- [x] 5.2 Написать юнит-тесты для `PriceCommand` с мокированными ответами InvestAPI
- [x] 5.3 Написать юнит-тесты для `ChartCommand` с мокированными данными свечей
- [x] 5.4 Запустить полную сборку `./gradlew clean build` и убедиться, что все тесты проходят
