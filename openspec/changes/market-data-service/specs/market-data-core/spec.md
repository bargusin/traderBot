## ADDED Requirements

### Requirement: Сервис рыночных данных оборачивает InvestAPI MarketDataService
Система SHALL предоставить класс `MarketDataService` в модуле `core`, оборачивающий `ru.tinkoff.piapi.core.MarketDataService` и предоставляющий async-методы для свечей, последних цен, стакана и статуса торгов.

#### Scenario: Получение свечей по figi инструмента
- **WHEN** вызван метод `getCandles(figi, from, to, interval)`
- **THEN** система SHALL возвращать `CompletableFuture<List<HistoricCandle>>`, делегируя вызов в InvestAPI `MarketDataService.getCandles`

#### Scenario: Получение последних цен по figi инструмента
- **WHEN** вызван метод `getLastPrices(figi)`
- **THEN** система SHALL возвращать `CompletableFuture<List<LastPrice>>`, делегируя вызов в InvestAPI `MarketDataService.getLastPrices`

#### Scenario: Получение стакана по figi инструмента
- **WHEN** вызван метод `getOrderBook(figi, depth)`
- **THEN** система SHALL возвращать `CompletableFuture<GetOrderBookResponse>`, делегируя вызов в InvestAPI `MarketDataService.getOrderBook`

#### Scenario: Получение статуса торгов по figi инструмента
- **WHEN** вызван метод `getTradingStatus(figi)`
- **THEN** система SHALL возвращать `CompletableFuture<GetTradingStatusResponse>`, делегируя вызов в InvestAPI `MarketDataService.getTradingStatus`

#### Scenario: Поиск инструмента по тикеру
- **WHEN** вызван метод `findInstrumentByTicker(ticker)`
- **THEN** система SHALL вызывать `InstrumentsService.findInstrument(ticker)` и возвращать первый найденный инструмент с его figi, или пустое значение, если не найден

### Requirement: Ответы рыночных данных кешируются
Система SHALL кешировать ответы рыночных данных in-memory с настраиваемым TTL для каждого типа данных, чтобы уменьшить количество вызовов API и соблюдать rate limits.

#### Scenario: Попадание в кеш в пределах TTL
- **WHEN** в кеше существует ответ для того же ключа запроса и TTL ещё не истёк
- **THEN** система SHALL возвращать кешированное значение без вызова InvestAPI

#### Scenario: Промах кеша или истёкший TTL
- **WHEN** в кеше нет ответа или TTL истёк
- **THEN** система SHALL вызвать InvestAPI, сохранить результат в кеш и вернуть его

#### Scenario: TTL кеша для свечей
- **WHEN** свечи кешируются
- **THEN** TTL SHALL составлять 300 секунд (5 минут)

#### Scenario: TTL кеша для последних цен
- **WHEN** последние цены кешируются
- **THEN** TTL SHALL составлять 60 секунд (1 минута)

#### Scenario: TTL кеша для стакана
- **WHEN** стакан кешируется
- **THEN** TTL SHALL составлять 600 секунд (10 минут)

#### Scenario: Кеш не превышает максимальный размер
- **WHEN** кеш содержит 500 записей и добавляется новая запись
- **THEN** система SHALL удалять самую старую запись (FIFO)

### Requirement: Сервис рыночных данных обрабатывает ошибки InvestAPI
Система SHALL перехватывать `ApiRuntimeException` и возвращать.failedFuture CompletableFuture с описательным сообщением об ошибке вместо проброса исключений.

#### Scenario: Вызов API завершился ошибкой сети
- **WHEN** вызов InvestAPI выбрасывает `ApiRuntimeException`
- **THEN** система SHALL возвращать `CompletableFuture.failedFuture()` с оригинальным сообщением об ошибке

#### Scenario: Вызов API завершился ошибкой авторизации
- **WHEN** вызов InvestAPI выбрасывает исключение с "UNAUTHENTICATED" или кодом "30052"
- **THEN** система SHALL пробрасывать ошибку для обработки на уровне выше в `ApiCallExecutor`
