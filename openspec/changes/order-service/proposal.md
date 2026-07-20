## Why

Бот умеет показывать портфель и рыночные данные, но не может размещать ордера. Пользователям необходимо покидать Telegram для совершения сделок в приложении Т-Банка. Интеграция с Tinkoff InvestAPI `OrdersService` позволит торговать прямо из бота — покупать, продавать и управлять активными ордерами.

## What Changes

- Новый сервисный слой `TradingService` в модуле `core`, оборачивающий Tinkoff InvestAPI `OrdersService` с обработкой ошибок и конвертацией типов
- Новая команда `/purchase <ticker>` — multi-step flow покупки инструмента (тикер → количество → подтверждение → исполнение)
- Новая команда `/sell <ticker>` — multi-step flow продажи инструмента (аналогичный флоу)
- Новая команда `/orders` — просмотр активных и исполненных ордеров с кнопкой отмены для активных
- Интеграция с Tinkoff InvestAPI `OrdersService` (postOrder, cancelOrder, getOrders, getOrderState, getOrderPrice, getMaxLots)
- Поиск инструмента по тикеру через существующий `InstrumentsService.findInstrument`

## Capabilities

### New Capabilities

- `trading-service`: Сервисный слой в core, оборачивающий Tinkoff InvestAPI OrdersService — размещение рыночных и лимитных ордеров, отмена, получение списка ордеров, предварительный расчёт стоимости и макс. доступных лотов
- `purchase-command`: Telegram-команда `/purchase <ticker>` — multi-step flow покупки с подтверждением через inline-кнопки
- `sell-command`: Telegram-команда `/sell <ticker>` — multi-step flow продажи с подтверждением через inline-кнопки
- `orders-command`: Telegram-команда `/orders` — просмотр активных и исполненных ордеров с кнопкой отмены для активных

### Modified Capabilities

- `help-command`: Добавить новые команды (/purchase, /sell, /orders) в справку

## Impact

- **Модуль core**: Новый класс `TradingService`, утилиты конвертации типов InvestAPI
- **Модуль bot**: Четыре новых класса команд (`PurchaseCommand`, `SellCommand`, `OrdersCommand`, `OrderCallbackHandler`), обновлена регистрация в `MessageHandler`, обновлён `HelpCommand`
- **Зависимости**: Новых нет — используется существующий `ru.tinkoff.piapi:java-sdk-core:1.27` (`getOrdersService`)
- **API**: T InvestAPI OrdersService (postOrder, cancelOrder, getOrders, getOrderState, getOrderPrice, getMaxLots)
