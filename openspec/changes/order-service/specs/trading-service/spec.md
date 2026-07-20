## ADDED Requirements

### Requirement: Place market order
The system SHALL allow placing market orders via InvestAPI OrdersService.postOrder with ORDER_TYPE_MARKET.

#### Scenario: Successful market buy order
- **WHEN** user confirms a BUY order with valid figi, lots, and account
- **THEN** system calls postOrder with ORDER_DIRECTION_BUY, ORDER_TYPE_MARKET
- **AND** returns PostOrderResponse with order ID and execution status

#### Scenario: Market order with invalid figi
- **WHEN** user attempts to place order with non-existent figi
- **THEN** system returns error message "Instrument not found"

### Requirement: Place limit order
The system SHALL allow placing limit orders via InvestAPI OrdersService.postOrder with ORDER_TYPE_LIMIT and explicit price.

#### Scenario: Successful limit buy order
- **WHEN** user confirms a LIMIT BUY order with figi, lots, price, and account
- **THEN** system calls postOrder with ORDER_DIRECTION_BUY, ORDER_TYPE_LIMIT, and Quotation price
- **AND** returns PostOrderResponse with order ID

### Requirement: Cancel order
The system SHALL allow cancelling active orders via InvestAPI OrdersService.cancelOrder.

#### Scenario: Successful cancel
- **WHEN** user requests cancel for an active order by order ID
- **THEN** system calls cancelOrder with accountId and orderId
- **AND** returns cancellation timestamp

#### Scenario: Cancel non-existent order
- **WHEN** user attempts to cancel order that doesn't exist or already executed
- **THEN** system returns error message with reason

### Requirement: Get active orders
The system SHALL retrieve list of active (non-executed) orders via InvestAPI OrdersService.getOrders.

#### Scenario: List active orders
- **WHEN** user requests active orders
- **THEN** system calls getOrders with accountId
- **AND** returns list of OrderState with order ID, status, lots, price, direction

### Requirement: Get order price estimate
The system SHALL calculate estimated order cost via InvestAPI OrdersService.getOrderPrice.

#### Scenario: Estimate buy order cost
- **WHEN** user specifies instrument, lots, and direction
- **THEN** system calls getOrderPrice
- **AND** returns estimated total cost, commission, and margin requirements

### Requirement: Get max lots
The system SHALL retrieve maximum available lots via InvestAPI OrdersService.getMaxLots.

#### Scenario: Check available lots
- **WHEN** user specifies instrument and price
- **THEN** system calls getMaxLots
- **AND** returns max available buy and sell lots
