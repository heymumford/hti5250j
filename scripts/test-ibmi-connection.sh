#!/usr/bin/env bash
# Comprehensive IBM i connection diagnostic and test
# Usage: set -a && source .env && set +a && ./scripts/test-ibmi-connection.sh

set -euo pipefail

# Load environment
if [ -f .env ]; then
    set -a
    source .env
    set +a
fi

HOST="${IBM_I_HOST:-}"
PORT="${IBM_I_PORT:-992}"
USER="${IBM_I_USER:-}"
if [[ -z "$HOST" ]]; then
    echo "error: IBM_I_HOST must be set" >&2
    exit 2
fi

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════╗"
echo "║     IBM i Connection Diagnostic & Test Suite            ║"
echo "╚════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Test 1: Network connectivity
echo ""
echo -e "${YELLOW}[TEST 1] Network Connectivity${NC}"
echo "Target: $HOST:$PORT"

if timeout 3 bash -c "echo >/dev/tcp/$HOST/$PORT" 2>/dev/null; then
    echo -e "${GREEN}✓ Port $PORT is open and reachable${NC}"
    PORT_OPEN=1
else
    echo -e "${RED}✗ Cannot reach $HOST:$PORT (timeout or refused)${NC}"
    echo "  This is expected if IBM i is on internal network only"
    PORT_OPEN=0
fi

# Test 2: DNS resolution
echo ""
echo -e "${YELLOW}[TEST 2] DNS Resolution${NC}"
if getent hosts "$HOST" >/dev/null 2>&1; then
    IP=$(getent hosts "$HOST" | awk '{print $1}')
    echo -e "${GREEN}✓ $HOST resolves to $IP${NC}"
else
    echo -e "${YELLOW}⚠ $HOST does not resolve (using IP directly)${NC}"
fi

# Test 3: Java environment
echo ""
echo -e "${YELLOW}[TEST 3] Java Environment${NC}"

if [ -z "$JAVA_HOME" ]; then
    if command -v /usr/libexec/java_home &> /dev/null; then
        JAVA_HOME=$(/usr/libexec/java_home)
    fi
fi

if [ -z "$JAVA_HOME" ]; then
    echo -e "${RED}✗ JAVA_HOME not set${NC}"
else
    JAVA_VERSION=$("$JAVA_HOME/bin/java" -version 2>&1 | head -1)
    echo -e "${GREEN}✓ $JAVA_VERSION${NC}"
    echo "  JAVA_HOME: $JAVA_HOME"

    KEYSTORE="$JAVA_HOME/lib/security/cacerts"
    if [ -f "$KEYSTORE" ]; then
        CERT_COUNT=$(keytool -list -keystore "$KEYSTORE" -storepass changeit 2>/dev/null | grep -c "^[^*]" || true)
        echo -e "${GREEN}✓ Keystore found with ~$CERT_COUNT certificates${NC}"
    else
        echo -e "${YELLOW}⚠ Default keystore not found at $KEYSTORE${NC}"
    fi
fi

# Test 4: SSL certificate
if [ "$PORT_OPEN" = "1" ]; then
    echo ""
    echo -e "${YELLOW}[TEST 4] SSL Certificate${NC}"

    CERT_FILE="/tmp/ibmi-test-cert.pem"
    if timeout 5 openssl s_client -connect "$HOST:$PORT" -showcerts </dev/null 2>/dev/null | \
       sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > "$CERT_FILE" 2>/dev/null && \
       [ -s "$CERT_FILE" ]; then

        echo -e "${GREEN}✓ SSL certificate retrieved${NC}"

        # Parse certificate info
        SUBJECT=$(openssl x509 -in "$CERT_FILE" -text -noout 2>/dev/null | grep "Subject:" | sed 's/.*Subject: //')
        ISSUER=$(openssl x509 -in "$CERT_FILE" -text -noout 2>/dev/null | grep "Issuer:" | sed 's/.*Issuer: //')
        NOT_BEFORE=$(openssl x509 -in "$CERT_FILE" -text -noout 2>/dev/null | grep "Not Before:" | sed 's/.*Not Before: //')
        NOT_AFTER=$(openssl x509 -in "$CERT_FILE" -text -noout 2>/dev/null | grep "Not After :" | sed 's/.*Not After : //')

        echo "  Subject: $SUBJECT"
        echo "  Issuer: $ISSUER"
        echo "  Valid From: $NOT_BEFORE"
        echo "  Valid Until: $NOT_AFTER"

        rm -f "$CERT_FILE"
    else
        echo -e "${RED}✗ Could not retrieve SSL certificate${NC}"
    fi
else
    echo ""
    echo -e "${YELLOW}[TEST 4] SSL Certificate${NC}"
    echo -e "${YELLOW}⊘ Skipped (port not reachable)${NC}"
fi

# Test 5: Environment variables
echo ""
echo -e "${YELLOW}[TEST 5] Configuration${NC}"
echo "  IBM_I_HOST: $HOST"
echo "  IBM_I_PORT: $PORT"
echo "  IBM_I_USER: ${USER:-[NOT SET]}"
echo "  IBM_I_DATABASE: ${IBM_I_DATABASE:-[NOT SET]}"
[ -z "$IBM_I_PASSWORD" ] && echo "  IBM_I_PASSWORD: [NOT SET]" || echo "  IBM_I_PASSWORD: [SET]"
echo "  IBM_I_SSL: ${IBM_I_SSL:-true}"

# Test 6: Run Gradle tests
echo ""
echo -e "${YELLOW}[TEST 6] Run Integration Tests${NC}"
echo ""

if command -v ./gradlew &> /dev/null; then
    if [ "$PORT_OPEN" = "1" ]; then
        echo -e "${GREEN}✓ Network available - running full integration tests${NC}"
        set +e
        ./gradlew test --tests "IBMiUATIntegrationTest" 2>&1 | tail -40
        TEST_EXIT=$?
        set -e
    else
        echo -e "${YELLOW}⚠ Network not available - running contract tests only${NC}"
        set +e
        ./gradlew test --tests "IBMiConnectionFactoryContractTest" 2>&1 | tail -30
        TEST_EXIT=$?
        set -e
    fi
else
    echo -e "${YELLOW}⚠ gradlew not found${NC}"
fi

# Summary
echo ""
echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}SUMMARY${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"

if [ "$PORT_OPEN" = "1" ]; then
    echo -e "${GREEN}✓ Network connectivity available${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. If using self-signed cert:"
    echo "     sudo ./scripts/install-ibmi-cert.sh $HOST $PORT"
    echo ""
    echo "  2. Run integration tests:"
    echo "     ./gradlew test --tests IBMiUATIntegrationTest"
    echo ""
    echo "  3. Check test output for telnet negotiation details"
else
    echo -e "${YELLOW}⚠ Network not reachable from this location${NC}"
    echo ""
    echo "The IBM i UAT system appears to be on an internal network."
    echo ""
    echo "When you have network access:"
    echo "  1. Run this script again:"
    echo "     set -a && source .env && set +a && ./scripts/test-ibmi-connection.sh"
    echo ""
    echo "  2. Install SSL certificate if needed:"
    echo "     sudo ./scripts/install-ibmi-cert.sh $HOST $PORT"
    echo ""
    echo "  3. Run integration tests:"
    echo "     set -a && source .env && set +a"
    echo "     ./gradlew test --tests IBMiUATIntegrationTest"
fi

echo -e "${BLUE}════════════════════════════════════════════════════════${NC}"
echo ""
