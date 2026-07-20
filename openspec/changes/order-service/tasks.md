## 1. Core: TradingService

- [x] 1.1 Create `TradingService` class in `core/src/main/java/ru/rapidcoder/trader/core/service/TradingService.java` wrapping InvestAPI `OrdersService`
- [x] 1.2 Implement `placeOrder(accountId, figi, lots, price, direction, orderType)` → `PostOrderResponse`
- [x] 1.3 Implement `cancelOrder(accountId, orderId)` → `Instant`
- [x] 1.4 Implement `getActiveOrders(accountId)` → `List<OrderState>` (filter non-executed)
- [x] 1.5 Implement `getAllOrders(accountId)` → `List<OrderState>` (all including executed)
- [x] 1.6 Implement `getOrderState(accountId, orderId)` → `OrderState`
- [x] 1.7 Implement `getOrderPrice(accountId, figi, lots, price, direction)` → `GetOrderPriceResponse`
- [x] 1.8 Implement `getMaxLots(accountId, figi, price)` → `GetMaxLotsResponse`
- [x] 1.9 Add error handling: map gRPC errors to user-friendly messages (insufficient funds, invalid lots, etc.)
- [x] 1.10 Add unit tests for `TradingService` in `core/src/test/java/ru/rapidcoder/trader/core/service/test/TradingServiceTest.java`

## 2. Bot: Constants and Registration

- [x] 2.1 Add constants to `BotConstants.java`: `CMD_PURCHASE`, `CMD_SELL`, `CMD_ORDERS`, `CALLBACK_CONFIRM_ORDER`, `CALLBACK_CANCEL_ORDER`, `CALLBACK_SWITCH_ORDER_TYPE`, `CALLBACK_ORDERS_HISTORY`
- [x] 2.2 Register `PurchaseCommand`, `SellCommand`, `OrdersCommand`, `OrderCallbackHandler` in `MessageHandler` constructor
- [x] 2.3 Update `HelpCommand` to include Trading category with `/purchase`, `/sell`, `/orders`

## 3. Bot: PurchaseCommand

- [x] 3.1 Create `PurchaseCommand` class extending `AbstractCommand` in `bot/src/main/java/ru/rapidcoder/trader/bot/command/trading/`
- [x] 3.2 Implement `/purchase` without arguments: prompt "Enter ticker:" with cancelKeyboard, register InputHandler
- [x] 3.3 Implement `/purchase <ticker>` with argument: skip ticker step, go directly to quantity
- [x] 3.4 Implement Step 1 - ticker lookup: search instrument, show price + max lots, ask quantity
- [x] 3.5 Implement Step 2 - quantity input: validate positive integer <= max lots, show estimated cost, show confirm/cancel buttons
- [x] 3.6 Implement Step 3 - confirm callback: place market BUY order, show result
- [x] 3.7 Handle cancel at any step via `/cancel` and inline "Cancel" button
- [x] 3.8 Handle instrument not found and insufficient lots errors
- [x] 3.9 Add unit tests for `PurchaseCommand` in `bot/src/test/java/ru/rapidcoder/trader/bot/command/test/PurchaseCommandTest.java`

## 4. Bot: SellCommand

- [x] 4.1 Create `SellCommand` class extending `AbstractCommand` in `bot/src/main/java/ru/rapidcoder/trader/bot/command/trading/`
- [x] 4.2 Implement `/sell` without arguments: prompt "Enter ticker:" with cancelKeyboard, register InputHandler
- [x] 4.3 Implement `/sell <ticker>` with argument: skip ticker step, go directly to quantity
- [x] 4.4 Implement Step 1 - ticker lookup: search instrument, show price + user's position, ask quantity
- [x] 4.5 Implement Step 2 - quantity input: validate positive integer <= position quantity, show estimated proceeds, show confirm/cancel buttons
- [x] 4.6 Implement Step 3 - confirm callback: place market SELL order, show result
- [x] 4.7 Handle cancel at any step via `/cancel` and inline "Cancel" button
- [x] 4.8 Handle instrument not found and insufficient position errors
- [x] 4.9 Add unit tests for `SellCommand` in `bot/src/test/java/ru/rapidcoder/trader/bot/command/test/SellCommandTest.java`

## 5. Bot: OrdersCommand

- [x] 5.1 Create `OrdersCommand` class extending `AbstractCommand` in `bot/src/main/java/ru/rapidcoder/trader/bot/command/trading/`
- [x] 5.2 Implement default view: show active orders with instrument, direction, lots, status, "Cancel" button per order
- [x] 5.3 Implement "History" button: switch to executed orders view
- [x] 5.4 Implement "Active" button: switch back to active orders view
- [x] 5.5 Implement cancel button callback: cancel order, show confirmation, refresh list
- [x] 5.6 Handle empty state: "No active orders" / "No executed orders"
- [x] 5.7 Add unit tests for `OrdersCommand` in `bot/src/test/java/ru/rapidcoder/trader/bot/command/test/OrdersCommandTest.java`

## 6. Bot: OrderCallbackHandler

- [x] 6.1 Create `OrderCallbackHandler` class in `bot/src/main/java/ru/rapidcoder/trader/bot/command/trading/`
- [x] 6.2 Handle `confirm_order` callback: parse payload (direction, ticker, lots), execute order
- [x] 6.3 Handle `cancel_order` callback: parse orderId, cancel order, notify user
- [x] 6.4 Handle `switch_order_type` callback: toggle between MARKET/LIMIT in purchase/sell flow

## 7. Integration and Build

- [x] 7.1 Run `./gradlew clean build` — verify all tests pass
- [x] 7.2 Verify `/help` shows all trading commands
- [ ] 7.3 Manual smoke test in sandbox: `/purchase` → ticker → qty → confirm → verify order placed
- [ ] 7.4 Manual smoke test in sandbox: `/sell` → ticker → qty → confirm → verify order placed
- [ ] 7.5 Manual smoke test in sandbox: `/orders` → verify list shows orders with cancel buttons
