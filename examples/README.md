# HTI5250J Workflow Examples

This directory contains example workflows that demonstrate HTI5250J workflow syntax and validation.

## Example Workflows

### login.yaml
Simple login workflow demonstrating basic authentication.

**Features:**
- LOGIN action (basic credentials)
- ASSERT action (verification)

**Usage:**
```bash
i5250 validate examples/login.yaml
```

### payment.yaml
Payment processing workflow with parameter binding from CSV data.

**Features:**
- LOGIN action (environment variables)
- NAVIGATE action (screen transition)
- FILL action (form field entry)
- SUBMIT action (keyboard submission)
- WAIT action (timeout handling)
- ASSERT action (result verification)
- CAPTURE action (screenshot)
- Parameter substitution (${data.*})

**Usage:**
```bash
# Validate workflow structure
i5250 validate examples/payment.yaml

# Validate workflow + dataset parameters
i5250 validate examples/payment.yaml --data examples/payment_data.csv

# Run workflow (when i5 integration available)
i5250 run examples/payment.yaml --data examples/payment_data.csv
```

### settlement.yaml
Settlement batch processing workflow for nightly reconciliation.

**Features:**
- Full workflow lifecycle
- Extended timeouts for batch processing
- Data-driven batch parameters
- Result capture for audit trail

**Usage:**
```bash
i5250 validate examples/settlement.yaml
i5250 validate examples/settlement.yaml --data examples/settlement_data.csv
```

## Data Files

### payment_data.csv
Sample payment transaction data:
- account_id: System account identifier
- amount: Transaction amount in currency units
- description: Invoice reference

### settlement_data.csv
Sample settlement batch parameters:
- batch_date: Processing date (YYYY-MM-DD)
- count: Transaction count in batch

## Validation

All examples are valid workflows that pass structural validation.

### What Gets Validated

**Workflow Structure:**
- Name is required and non-empty
- At least one step is required
- All required fields for each action type

**Step Validation:**
- Timeout bounds (100-300000 milliseconds)
- Action-specific constraints:
  - LOGIN: host, user, password required
  - NAVIGATE: screen required
  - FILL: fields map required
  - SUBMIT: key required
  - ASSERT: screen OR text required
  - WAIT: positive timeout required
  - CAPTURE: name recommended (warning only)

**Parameter Validation:**
- All ${data.*} references must exist in provided CSV
- Warnings generated for missing columns

### Running Validation

```bash
# Basic validation
i5250 validate payment.yaml

# With dataset
i5250 validate payment.yaml --data payment_data.csv

# Check all examples
for f in *.yaml; do
  echo "Validating $f..."
  i5250 validate "$f"
done
```

## Workflow Execution

All example workflows can be executed against a real IBM i system.

### Running Workflows

```bash
# Validate workflow before running
i5250 validate payment.yaml --data payment_data.csv

# Execute workflow
i5250 run payment.yaml --data payment_data.csv
```

### Execution Output

Successful execution produces:

```
✓ Step 0: LOGIN - Connected and authenticated
✓ Step 1: NAVIGATE - Reached payment entry screen
✓ Step 2: FILL - Populated form fields
✓ Step 3: SUBMIT - Submitted and awaited response
✓ Step 4: ASSERT - Confirmed successful transaction
✓ Step 5: CAPTURE - Screenshot saved

Artifacts saved to:
  - artifacts/ledger.txt: Execution log
  - artifacts/screenshots/step_0_login.txt
  - artifacts/screenshots/step_4_assert.txt
  - artifacts/screenshots/step_5_capture.txt
```

### Error Handling

If a step fails, execution stops with error context:

```
✗ Step 1: NAVIGATE - Failed to reach target screen
  Current screen: MAIN MENU
  Expected: PAYMENT ENTRY
  Timeout: 10000ms

Artifacts for debugging:
  - artifacts/screenshots/step_1_failure.txt (includes full screen dump)
  - artifacts/ledger.txt (all previous steps)
```

Exception types:
- `NavigationException`: Could not navigate to target screen
- `AssertionException`: Content verification failed (includes screen dump)
- `TimeoutException`: Keyboard or screen operation timed out

### Parameter Substitution in Execution

When you execute a workflow with a CSV dataset, `${data.COLUMN}` references are replaced with actual values:

**payment_data.csv:**
```
account_id,amount,description
ACC001,150.00,Invoice-2026-001
ACC002,275.50,Invoice-2026-002
```

**payment.yaml step (FILL action):**
```yaml
fields:
  account: "${data.account_id}"
  amount: "${data.amount}"
```

**Execution substitutes:**
- First row: account="ACC001", amount="150.00"
- Second row: account="ACC002", amount="275.50"

### Artifacts Produced

Each execution produces text files for debugging:

**ledger.txt (execution timeline):**
```
2026-02-08 14:30:15.123 [LOGIN] Connecting to ibmi.example.com:23
2026-02-08 14:30:15.456 [LOGIN] Keyboard unlocked, ready for input
2026-02-08 14:30:15.478 [NAVIGATE] Sending: CALL PGM(PMTENT)<ENTER>
2026-02-08 14:30:16.234 [NAVIGATE] Screen verified: Payment Entry
2026-02-08 14:30:16.245 [FILL] Populated fields: account, amount
2026-02-08 14:30:16.456 [SUBMIT] Sending: [ENTER]
2026-02-08 14:30:17.123 [SUBMIT] Keyboard lock→unlock detected
2026-02-08 14:30:17.145 [ASSERT] Verified: "Transaction accepted"
2026-02-08 14:30:17.234 [CAPTURE] Screenshot saved: step_5_capture.txt
```

**screenshots/step_4_assert.txt (screen dump, 80 columns):**
```
PAYMENT CONFIRMATION SCREEN                              [Page 1 of 1]

Transaction ID: TXN-2026-001
Account:       ACC001
Amount:        150.00
Description:   Invoice-2026-001
Date:          2026-02-08

Status: ACCEPTED ✓

Press ENTER to continue...
```

## Troubleshooting

### Navigation Failed

**Error:** `NavigationException: Could not navigate to PAYMENT ENTRY`

**Causes:**
1. Screen name doesn't match actual screen title
2. Timeout too short for system response
3. Keystroke sequence invalid for this menu

**Fix:**
1. Check actual screen content in screenshot artifact
2. Increase timeout: change `timeout: 5000` to `timeout: 10000`
3. Verify keystroke sequence matches your system

### Assertion Failed

**Error:** `AssertionException: Screen did not contain: "Transaction accepted"`

**Causes:**
1. Text not visible (case sensitivity?)
2. FILL/SUBMIT didn't complete properly
3. Screen hasn't refreshed yet

**Fix:**
1. Check screenshot artifact (screenshots/step_4_assert.txt)
2. Verify field values in CSV data
3. Add WAIT step before ASSERT: `action: WAIT` with increased timeout

### Timeout

**Error:** `TimeoutException: Keyboard unlock timeout`

**Cause:** IBM i system is slow or stuck

**Fix:**
1. Increase timeout in step: `timeout: 30000` (30 seconds)
2. Check IBM i system status (might be overloaded)
3. Reduce concurrent workflows (resource exhaustion?)

### Connection Failed

**Error:** `Exception: Could not connect to ibmi.example.com:23`

**Cause:** Network connectivity or wrong host

**Fix:**
1. Verify host name/IP is correct
2. Check firewall allows port 23 (or 992 for SSL)
3. Verify IBM i is running and accepting connections
4. Test connection: `telnet ibmi.example.com 23`

## Creating Your Own Workflows

1. Define workflow name and description
2. Create steps in sequence
3. Choose appropriate actions for each step
4. Fill in required fields per action type
5. Use ${data.column} for CSV parameter binding
6. Validate before running: `i5250 validate myworkflow.yaml`
7. Execute: `i5250 run myworkflow.yaml --data mydata.csv`

See the examples above for reference implementations.

## Architecture & Testing

For detailed information on system architecture and testing strategy, see:
- [ARCHITECTURE.md](../ARCHITECTURE.md) -- System design and workflow execution pipeline
- [TESTING.md](../TESTING.md) -- Four-domain test framework
