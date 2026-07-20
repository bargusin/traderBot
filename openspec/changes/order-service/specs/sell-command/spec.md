## ADDED Requirements

### Requirement: Sell command with arguments
The system SHALL support `/sell <ticker>` syntax that skips the ticker input step and goes directly to quantity.

#### Scenario: Sell with ticker argument
- **WHEN** user sends `/sell AAPL`
- **THEN** system looks up instrument by ticker "AAPL"
- **AND** skips ticker input step, shows price and asks for quantity

### Requirement: Sell command without arguments
The system SHALL support `/sell` without arguments, prompting user to enter a ticker.

#### Scenario: Sell without argument
- **WHEN** user sends `/sell`
- **THEN** system sends message "Enter ticker:" with cancel button

### Requirement: Multi-step sell flow
The system SHALL implement a 3-step sell flow: ticker → quantity → confirm.

#### Scenario: Step 1 - ticker lookup
- **WHEN** user enters ticker (via argument or InputHandler)
- **THEN** system searches instrument by ticker/FIGI/ISIN
- **AND** shows instrument name, current price, and user's position quantity
- **AND** asks user to enter quantity to sell

#### Scenario: Step 2 - quantity validation
- **WHEN** user enters quantity
- **THEN** system validates quantity is positive integer and <= user's position quantity
- **AND** shows estimated total proceeds
- **AND** shows inline buttons: "Confirm" and "Cancel"

#### Scenario: Step 3 - confirm order
- **WHEN** user clicks "Confirm" button
- **THEN** system places market SELL order via TradingService
- **AND** shows result: order ID, status, executed lots

#### Scenario: Cancel at any step
- **WHEN** user sends /cancel or clicks "Cancel" button at any step
- **THEN** system clears state and sends "Sell cancelled"

### Requirement: Instrument not found
The system SHALL handle unknown tickers gracefully.

#### Scenario: Ticker not found
- **WHEN** user enters ticker that doesn't match any instrument
- **THEN** system sends "Instrument not found" message
- **AND** clears state

### Requirement: Insufficient position
The system SHALL validate quantity against user's current position.

#### Scenario: Quantity exceeds position
- **WHEN** user enters quantity greater than their position quantity
- **THEN** system sends "Insufficient position. You have: X lots"
- **AND** keeps state for re-entry
