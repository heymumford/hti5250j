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

## Creating Your Own Workflows

1. Define workflow name and description
2. Create steps in sequence
3. Choose appropriate actions for each step
4. Fill in required fields per action type
5. Use ${data.column} for CSV parameter binding
6. Validate before running: `i5250 validate myworkflow.yaml`

See the examples above for reference implementations.
