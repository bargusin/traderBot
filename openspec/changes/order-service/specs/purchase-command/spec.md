## ADDED Requirements

### Requirement: Purchase command with arguments
The system SHALL support `/purchase <ticker>` syntax that skips the ticker input step and goes directly to quantity.

#### Scenario: Purchase with ticker argument
- **WHEN** user sends `/purchase AAPL`
- **THEN** system looks up instrument by ticker "AAPL"
- **AND** skips ticker input step, shows price and asks for quantity

### Requirement: Purchase command without arguments
The system SHALL support `/purchase` without arguments, prompting user to enter a ticker.

#### Scenario: Purchase without argument
- **WHEN** user sends `/purchase`
- **THEN** system sends message "Enter ticker:" with cancel button

### Requirement: Multi-step purchase flow
The system SHALL implement a 3-step purchase flow: ticker → quantity → confirm.

#### Scenario: Step 1 - ticker lookup
- **WHEN** user enters ticker (via argument or InputHandler)
- **THEN** system searches instrument by ticker/FIGI/ISIN
- **AND** shows instrument name, current price, and available max lots
- **AND** asks user to enter quantity

#### Scenario: Step 2 - quantity validation
- **WHEN** user enters quantity
- **THEN** system validates quantity is positive integer and <= max lots
- **AND** shows estimated total cost and commission
- **AND** shows inline buttons: "Confirm" and "Cancel"

#### Scenario: Step 3 - confirm order
- **WHEN** user clicks "Confirm" button
- **THEN** system places market BUY order via TradingService
- **AND** shows result: order ID, status, executed lots

#### Scenario: Cancel at any step
- **WHEN** user sends /cancel or clicks "Cancel" button at any step
- **THEN** system clears state and sends "Purchase cancelled"

### Requirement: Instrument not found
The system SHALL handle unknown tickers gracefully.

#### Scenario: Ticker not found
- **WHEN** user enters ticker that doesn't match any instrument
- **THEN** system sends "Instrument not found" message
- **AND** clears state

### Requirement: Insufficient lots
The system SHALL validate quantity against available lots.

#### Scenario: Quantity exceeds max lots
- **WHEN** user enters quantity greater than max available lots
- **THEN** system sends "Insufficient lots. Max available: X"
- **AND** keeps state for re-entry
