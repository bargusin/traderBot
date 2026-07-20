## ADDED Requirements

### Requirement: List active orders
The system SHALL display active (non-executed) orders by default when user sends `/orders`.

#### Scenario: Show active orders
- **WHEN** user sends `/orders`
- **THEN** system calls getOrders with accountId
- **AND** displays list of active orders with: instrument name, direction (BUY/SELL), lots, status, order ID
- **AND** shows "Cancel" inline button next to each active order
- **AND** shows "History" button to switch to executed orders

#### Scenario: No active orders
- **WHEN** user sends `/orders` and there are no active orders
- **THEN** system sends "No active orders"

### Requirement: View executed orders
The system SHALL allow switching to view executed/completed orders via inline button.

#### Scenario: Switch to history
- **WHEN** user clicks "History" button on active orders view
- **THEN** system displays recently executed orders with: instrument, direction, lots, execution price, status
- **AND** shows "Active" button to switch back

#### Scenario: No executed orders
- **WHEN** user switches to history and there are no executed orders
- **THEN** system sends "No executed orders"

### Requirement: Cancel order from orders list
The system SHALL allow cancelling individual active orders via inline button.

#### Scenario: Cancel active order
- **WHEN** user clicks "Cancel" button next to an active order
- **THEN** system calls cancelOrder with orderId
- **AND** shows confirmation "Order X cancelled"
- **AND** refreshes the orders list

#### Scenario: Cancel fails
- **WHEN** user clicks "Cancel" but order was already executed or cancelled
- **THEN** system shows error message "Cannot cancel: order already executed"
