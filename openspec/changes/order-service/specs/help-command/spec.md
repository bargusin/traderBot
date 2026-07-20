## MODIFIED Requirements

### Requirement: Help command shows all available commands
The system SHALL display a categorized list of all available commands including trading commands.

#### Scenario: User requests help
- **WHEN** user sends /help
- **THEN** system displays help message with commands organized in categories
- **AND** categories include: Basic (/start, /help, /cancel), Accounts (/account, /portfolio), Market Data (/price, /chart), Trading (/purchase, /sell, /orders), Settings (/settings)
