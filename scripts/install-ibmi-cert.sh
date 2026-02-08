#!/usr/bin/env bash
# Install IBM i SSL certificate into Java truststore
# Usage: ./scripts/install-ibmi-cert.sh [host] [port]
# Example: ./scripts/install-ibmi-cert.sh ibmi.example.com 992

set -euo pipefail

HOST="${1:-${IBM_I_HOST:-}}"
PORT="${2:-${IBM_I_PORT:-992}}"
if [[ -z "$HOST" ]]; then
    echo "error: host is required (arg1 or IBM_I_HOST)" >&2
    exit 2
fi

CERT_FILE="/tmp/ibmi-${HOST}-${PORT}.pem"
CERT_ALIAS="ibmi-uat-$(date +%s)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}IBM i SSL Certificate Installer${NC}"
echo "=================================="
echo "Target: $HOST:$PORT"
echo ""

# Step 1: Extract certificate
echo -e "${YELLOW}Step 1: Extracting certificate from $HOST:$PORT...${NC}"
if timeout 10 openssl s_client -connect "$HOST:$PORT" -showcerts </dev/null 2>/dev/null | \
   sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > "$CERT_FILE"; then

    CERT_COUNT=$(grep -c "BEGIN CERTIFICATE" "$CERT_FILE")
    if [ "$CERT_COUNT" -eq 0 ]; then
        echo -e "${RED}✗ No certificates found. Check connectivity and port.${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Extracted $CERT_COUNT certificate(s)${NC}"
else
    echo -e "${RED}✗ Failed to connect. Check host and port.${NC}"
    exit 1
fi

# Step 2: Display certificate info
echo ""
echo -e "${YELLOW}Step 2: Certificate Information${NC}"
openssl x509 -in "$CERT_FILE" -text -noout | grep -E "Subject:|Issuer:|Not Before|Not After|CN="

# Step 3: Find Java keystore
echo ""
echo -e "${YELLOW}Step 3: Locating Java truststore...${NC}"

# Try to find JAVA_HOME
if [ -z "$JAVA_HOME" ]; then
    if command -v /usr/libexec/java_home &> /dev/null; then
        JAVA_HOME=$(/usr/libexec/java_home)
    else
        JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
    fi
fi

if [ -z "$JAVA_HOME" ]; then
    echo -e "${RED}✗ Could not find JAVA_HOME${NC}"
    exit 1
fi

KEYSTORE="$JAVA_HOME/lib/security/cacerts"
if [ ! -f "$KEYSTORE" ]; then
    echo -e "${RED}✗ Java truststore not found at: $KEYSTORE${NC}"
    echo "Try setting JAVA_HOME manually"
    exit 1
fi

echo -e "${GREEN}✓ Found: $KEYSTORE${NC}"

# Step 4: Import certificate
echo ""
echo -e "${YELLOW}Step 4: Importing certificate into Java truststore...${NC}"
echo "Certificate alias: $CERT_ALIAS"
echo "(Default password: 'changeit')"
echo ""

if keytool -import -alias "$CERT_ALIAS" -file "$CERT_FILE" -keystore "$KEYSTORE" -trustcacerts; then
    echo -e "${GREEN}✓ Certificate imported successfully${NC}"
    echo ""
    echo -e "${GREEN}Certificate installed! The Java application can now connect to:${NC}"
    echo "  Host: $HOST"
    echo "  Port: $PORT"
    echo ""
    echo -e "${YELLOW}To verify:${NC}"
    echo "  keytool -list -v -keystore \"$KEYSTORE\" | grep -A5 \"$CERT_ALIAS\""
else
    echo -e "${RED}✗ Failed to import certificate${NC}"
    exit 1
fi

# Cleanup
rm -f "$CERT_FILE"
echo -e "${GREEN}✓ Cleanup complete${NC}"
