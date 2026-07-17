## Почему

Бот目前 только показывает портфель по запросу, но не имеет доступа к рыночным данным в реальном времени. Пользователи не могут проверить цены на инструменты, посмотреть графики или проанализировать ликвидность, не выходя из Telegram. Этот функционал является основой для последующей реализации торговых стратегий и автоматических ордеров.

## Что меняется

- Новый обёртка `MarketDataService` в модуле `core` с in-memory кешем для рыночных данных (свечи, последние цены, стакан, статус торгов)
- Новая команда `/chart <ticker>` — интерактивный свечной график за выбранный период (1ч, 1д, 1н) с inline-кнопками выбора периода
- Новая команда `/price <ticker>` — текущая цена, дневное изменение, статус торгов и спред по инструменту
- Интеграция с Tinkoff InvestAPI `MarketDataService` (getCandles, getLastPrices, getOrderBook, getTradingStatus)
- Поиск инструмента по тикеру через существующий `InstrumentsService.findInstrument`

## Возможности

### Новые возможности
- `market-data-core`: Сервисный слой в `core`, оборачивающий Tinkoff InvestAPI MarketDataService с кешированием, обработкой ошибок и поддержкой async
- `chart-command`: Telegram-команда `/chart <ticker>` — отрисовка свечных данных в виде форматированного текста с выбором периода
- `price-command**: Telegram-команда `/price <ticker>` — отображение текущей цены, дневного изменения, объёма и статуса торгов

### Изменённые возможности

## Влияние

- **Модуль core**: Новый класс `MarketDataService`, утилита кеша
- **Модуль bot**: Два новых класса команд (`ChartCommand`, `PriceCommand`), обновлена регистрация в `MessageHandler`
- **Зависимости**: Новых нет — используется существующий `ru.tinkoff.piapi:java-sdk-core:1.27` (`getMarketDataService`)
- **API**: T InvestAPI MarketDataService (getCandles, getLastPrices, getOrderBook, getTradingStatus, getInstrumentByFigi)
