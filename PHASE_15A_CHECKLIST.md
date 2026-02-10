# Phase 15A: Environment Setup & Discovery Checklist

**Date Started:** February 10, 2026
**Status:** IN PROGRESS
**Goal:** Inventory IBM i systems and establish test environment

---

## Step 1: Identify Available IBM i System

### Check for Local VM/Simulator
```bash
# Look for common IBM i VM or simulator locations
ls -la /vm/ibmi* 2>/dev/null || echo "No local VMs found in /vm"
ls -la ~/ibmi* 2>/dev/null || echo "No local VMs found in ~"
docker ps 2>/dev/null | grep ibm || echo "No Docker IBM i containers found"
```

**Questions to Answer:**
- [ ] Do you have a local IBM i VM installed?
- [ ] Is it currently running?
- [ ] What's the hostname/IP address?
- [ ] What OS version? (7.3, 7.4, 7.5?)

### Check for Cloud Credentials
```bash
# Check for AWS IBM i instance
aws ec2 describe-instances --filters "Name=tag:Product,Values=IBMi" 2>/dev/null || echo "AWS not configured"

# Check for Azure
az vm list --query "[?tags.Product=='IBMi']" 2>/dev/null || echo "Azure not configured"

# Check for IBM Cloud
ibmcloud is instances 2>/dev/null || echo "IBM Cloud not configured"
```

**Questions to Answer:**
- [ ] Do you have cloud IBM i instances?
- [ ] Which cloud provider?
- [ ] What's the public/private IP?
- [ ] What's the pricing/budget?

### Check for Production System
```bash
# Look for production system documentation
find ~ -name "*ibm*" -type f 2>/dev/null | grep -i "prod\|prod-\|prd" | head -5
```

**Questions to Answer:**
- [ ] Do you have production IBM i system access?
- [ ] Is it approved for testing?
- [ ] What's the approval process?
- [ ] Is there a sandbox/test environment instead?

---

## Step 2: Test Network Connectivity

### TN5250E Port Availability
```bash
# Function to test port accessibility
test_port() {
    local host=$1
    local port=$2
    echo "Testing $host:$port..."
    timeout 3 bash -c "echo > /dev/tcp/$host/$port" 2>/dev/null && echo "✅ OPEN" || echo "❌ CLOSED"
}

# Test standard ports
test_port "YOUR_IBM_I_HOST" 23    # Telnet (plain)
test_port "YOUR_IBM_I_HOST" 992   # TLS (recommended)
test_port "YOUR_IBM_I_HOST" 8080  # HTTP (if available)
```

**After you provide hostname, run:**
```bash
IBM_I_HOST="your-ibm-i-hostname-or-ip"
timeout 3 bash -c "echo > /dev/tcp/$IBM_I_HOST/23" && echo "Port 23 open" || echo "Port 23 closed"
timeout 3 bash -c "echo > /dev/tcp/$IBM_I_HOST/992" && echo "Port 992 open" || echo "Port 992 closed"
```

**Questions to Answer:**
- [ ] Which port is accessible? (23 or 992?)
- [ ] Is firewall configured to allow connection?
- [ ] Do you need VPN to access?

---

## Step 3: Establish Test Credentials

### Secure Credential Storage

**NEVER commit credentials to git!** Store in `~/.env`:

```bash
# Create ~/.env with proper permissions
cat > ~/.env << 'ENVEOF'
# IBM i Test Environment (Phase 15)
IBM_I_HOST=192.168.1.100        # Change to your system
IBM_I_PORT=23                   # 23=telnet, 992=TLS
IBM_I_USER=TESTUSER             # Test account (not production!)
IBM_I_PASS=TESTPASS123          # Change immediately after testing
IBM_I_CCSID=37                  # USA (most common)
IBM_I_LIB=TESTLIB               # Test library/schema
ENVEOF

chmod 600 ~/.env
echo "✅ Credentials stored in ~/.env (600 perms)"
```

### Test Credentials
```bash
# Source the environment
source ~/.env

# Test login via telnet (manual)
echo "Testing credentials..."
(echo "$IBM_I_USER"; sleep 1; echo "$IBM_I_PASS"; sleep 1; echo "exit") | \
  telnet "$IBM_I_HOST" "$IBM_I_PORT" 2>&1 | head -20

# If successful, you should see IBM i welcome screen
```

**Questions to Answer:**
- [ ] Do you have a test account on IBM i?
- [ ] What's the username?
- [ ] What's the password?
- [ ] Is this account read-only or can it execute commands?
- [ ] What library/schema should tests use? (e.g., TESTLIB, DEVLIB)

---

## Step 4: Identify Available Test Programs

### Check What's Available on IBM i
```bash
# After successful login, check available programs
# (You'll need to run this interactively on IBM i system)

# In IBM i menu, check for:
# [ ] PMTENT (Payment Entry) - from Phase 1-14 test scenarios
# [ ] LNINQ  (Line Inquiry) - from Phase 1-14 test scenarios
# [ ] WRKSYSVAL (Work with System Values) - standard IBM i command
# [ ] Other menu systems

WRKOBJ OBJ(TESTLIB) OBJTYPE(*PGM)  # List all programs in test library
```

**Questions to Answer:**
- [ ] Is PMTENT available?
- [ ] Is LNINQ available?
- [ ] What other test programs are available?
- [ ] What's the database structure? (table names, fields)
- [ ] Can we modify test data without affecting production?

---

## Step 5: Document System Metadata

### IBM i System Information
```bash
# Get system info from IBM i (interactive)
DSPSYSVAL SYSVAL(QOSVERSION)    # IBM i OS version (e.g., 7.5)
DSPSYSVAL SYSVAL(QCCSID)        # System CCSID
DSPSYSVAL SYSVAL(QMAXSGNACN)    # Max sign-on attempts
DSPSYSVAL SYSVAL(QINACTITV)     # Inactivity interval
```

**Create documentation file:**
```bash
cat > docs/IBM_I_TEST_ENVIRONMENT.md << 'DOCEOF'
# IBM i Test Environment (Phase 15)

## System Information
- **Hostname/IP:** (from Step 1)
- **OS Version:** (from DSPSYSVAL)
- **CCSID:** 37 (USA)
- **TN5250E Port:** 23 or 992
- **Test Library:** TESTLIB

## Available Programs
- [ ] PMTENT (Payment Entry)
- [ ] LNINQ (Line Inquiry)
- [ ] WRKSYSVAL (Work with System Values)
- [ ] [Others]

## Test Account
- **Username:** (from Step 3)
- **Permissions:** Read-only / Can execute
- **Max sessions:** ?
- **Inactivity timeout:** ?

## Network Configuration
- **VPN Required:** Yes / No
- **Firewall Rules:** (describe any restrictions)
- **TLS Certificate:** Required / Optional

## Phase 15 Test Coverage
- [ ] Protocol negotiation (D2-PROTO-INIT)
- [ ] Screen format (D2-SCHEMA-SCREEN)
- [ ] Field attributes (D2-SCHEMA-FIELD)
- [ ] Command execution (D2-ACTION-CMD)
- [ ] Keyboard state (D2-CONCUR-KBD)

DOCEOF
cat docs/IBM_I_TEST_ENVIRONMENT.md
```

---

## Step 6: Create Phase 15A Summary

After completing Steps 1-5, create summary:

```bash
cat > PHASE_15A_RESULTS.md << 'RESULTEOF'
# Phase 15A Results: Environment Setup & Discovery

**Date:** February 10, 2026
**Status:** COMPLETE

## System Identified
- **Type:** [VM/Cloud/Production]
- **Hostname:** [your-ibm-i-host]
- **Port:** [23/992]
- **OS Version:** [7.3/7.4/7.5]
- **CCSID:** 37 (USA)

## Connectivity Verified
- [x] Port 23/992 accessible
- [x] Test account login successful
- [x] Can execute WRKSYSVAL

## Programs Available
- [x] PMTENT (Payment Entry)
- [x] LNINQ (Line Inquiry)
- [x] WRKSYSVAL (System Values)

## Ready for Phase 15B
- [x] Credentials in ~/.env (600 perms)
- [x] Documentation in docs/IBM_I_TEST_ENVIRONMENT.md
- [x] Network connectivity verified
- [x] Can proceed with D2-PROTO-INIT tests

## Phase 15B Blockers
(None identified)

RESULTEOF
cat PHASE_15A_RESULTS.md
```

---

## Contingency: IBM i Not Available

**If you don't have IBM i access:**

1. **Defer Phase 15** → Execute Phase 3 (SessionConfig migration) instead
2. **Document Gap** → Update PROTOCOL_RESEARCH.md with "Phase 15 deferred until infrastructure ready"
3. **Plan Phase 16** → Infrastructure requirements for future IBM i testing

```bash
cat > PHASE_15_DEFERRED.md << 'DEFEREOF'
# Phase 15 Deferred

**Date:** February 10, 2026
**Reason:** IBM i system not available

## Next Steps
1. Acquire IBM i infrastructure (VM, cloud, or production access)
2. Document credentials and system info
3. Return to Phase 15A when ready

## Alternative Work
- Execute Phase 3: SessionConfig API migration (15-20 hours)
- This unblocks future Java version upgrades

DEFEREOF
```

---

## What You Need to Do

**Answer these questions:**

1. **Do you have IBM i access?** (Yes/No)
   - If YES: What type? (Local VM, Cloud, Production)
   - If NO: Should we defer Phase 15 and work on Phase 3 instead?

2. **Provide system details:**
   - Hostname or IP address
   - Port (23 or 992)
   - Test account username
   - Test account password

3. **What programs are available?**
   - PMTENT?
   - LNINQ?
   - Others?

---

## Timeline

| Step | Time | Status |
|------|------|--------|
| 1. Identify system | 10 min | ⏳ Needs your input |
| 2. Test connectivity | 5 min | ⏳ Needs your input |
| 3. Establish credentials | 10 min | ⏳ Needs your input |
| 4. Identify programs | 15 min | ⏳ Needs your input |
| 5. Document metadata | 15 min | ⏳ Needs your input |
| 6. Create summary | 5 min | ⏳ Needs your input |
| **TOTAL** | **1 hour** | ⏳ Awaiting your input |

---

**Next Step:** Provide IBM i system details above, or confirm that Phase 15 should be deferred.

