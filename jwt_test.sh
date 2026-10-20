#!/bin/bash

################################################################################
# JWT Login & GetBooks Automated Test Script
# Complete testing with Keycloak JWT authentication
################################################################################

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
KEYCLOAK_URL="${1:-http://localhost:8180}"
API_URL="${2:-http://localhost:8080}"
CLIENT_ID="books-service"
CLIENT_SECRET="books-secret-2024"
USERNAME="user"
PASSWORD="user123"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_FILE="${SCRIPT_DIR}/jwt_test_$(date +%Y%m%d_%H%M%S).log"

################################################################################
# Helper Functions
################################################################################

log() {
    echo -e "${BLUE}[$(date '+%H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

success() {
    echo -e "${GREEN}✅ $1${NC}" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}❌ $1${NC}" | tee -a "$LOG_FILE"
}

warning() {
    echo -e "${YELLOW}⚠️  $1${NC}" | tee -a "$LOG_FILE"
}

info() {
    echo -e "${CYAN}ℹ️  $1${NC}" | tee -a "$LOG_FILE"
}

separator() {
    echo -e "${BLUE}════════════════════════════════════════════════════════${NC}" | tee -a "$LOG_FILE"
}

################################################################################
# Check Prerequisites
################################################################################

check_prerequisites() {
    separator
    log "Checking prerequisites..."

    # Check jq
    if ! command -v jq &> /dev/null; then
        error "jq is not installed"
        warning "Install with: brew install jq (macOS) or apt-get install jq (Linux)"
        exit 1
    fi
    success "jq is installed"

    # Check curl
    if ! command -v curl &> /dev/null; then
        error "curl is not installed"
        exit 1
    fi
    success "curl is installed"

    # Check connectivity
    log "Checking Keycloak connectivity..."
    if curl -s "$KEYCLOAK_URL" > /dev/null 2>&1; then
        success "Keycloak is reachable at $KEYCLOAK_URL"
    else
        error "Cannot reach Keycloak at $KEYCLOAK_URL"
        warning "Make sure Keycloak is running: docker-compose up -d"
        exit 1
    fi

    log "Checking API connectivity..."
    if curl -s "$API_URL/api/public/info" > /dev/null 2>&1; then
        success "API is reachable at $API_URL"
    else
        error "Cannot reach API at $API_URL"
        warning "Make sure application is running: docker-compose up -d"
        exit 1
    fi
}

################################################################################
# Login Function
################################################################################

login() {
    separator
    log "Authenticating user: $USERNAME"
    info "Connecting to: $KEYCLOAK_URL/realms/books-realm/protocol/openid-connect/token"

    RESPONSE=$(curl -s -X POST \
        "${KEYCLOAK_URL}/realms/books-realm/protocol/openid-connect/token" \
        -H 'Content-Type: application/x-www-form-urlencoded' \
        -d "grant_type=password" \
        -d "client_id=${CLIENT_ID}" \
        -d "client_secret=${CLIENT_SECRET}" \
        -d "username=${USERNAME}" \
        -d "password=${PASSWORD}" \
        -d "scope=openid profile email")

    # Check if response has error
    if echo "$RESPONSE" | jq -e '.error' > /dev/null 2>&1; then
        error "Authentication failed!"
        error "Error: $(echo "$RESPONSE" | jq -r '.error')"
        error "Description: $(echo "$RESPONSE" | jq -r '.error_description')"
        return 1
    fi

    # Extract token and metadata
    TOKEN=$(echo "$RESPONSE" | jq -r '.access_token')
    REFRESH_TOKEN=$(echo "$RESPONSE" | jq -r '.refresh_token')
    EXPIRES_IN=$(echo "$RESPONSE" | jq -r '.expires_in')
    TOKEN_TYPE=$(echo "$RESPONSE" | jq -r '.token_type')

    if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
        error "Failed to extract token from response"
        error "Response: $RESPONSE"
        return 1
    fi

    success "Authentication successful!"
    info "Token Type: $TOKEN_TYPE"
    info "Expires In: $EXPIRES_IN seconds"
    info "Token: ${TOKEN:0:50}..."

    return 0
}

################################################################################
# GetBooks Function
################################################################################

get_books() {
    separator
    log "Fetching books from API..."
    info "Endpoint: $API_URL/api/books"

    BOOKS=$(curl -s -X GET \
        "${API_URL}/api/books" \
        -H "Authorization: Bearer ${TOKEN}" \
        -H 'Content-Type: application/json')

    # Check if response is an array
    if ! echo "$BOOKS" | jq -e 'type == "array"' > /dev/null 2>&1; then
        error "Failed to get books!"
        error "Response: $BOOKS"
        return 1
    fi

    # Count books
    BOOK_COUNT=$(echo "$BOOKS" | jq 'length')

    success "Books retrieved successfully!"
    info "Total books: $BOOK_COUNT"

    return 0
}

################################################################################
# Display Books Function
################################################################################

display_books() {
    separator
    log "Top 5 Books:"
    echo ""

    echo "$BOOKS" | jq -r '.[0:5] | .[] | "  ID: \(.id), Title: \(.title), Author: \(.author), Price: \(.price)"' | while read -r line; do
        echo -e "${CYAN}$line${NC}"
    done
    echo ""
}

################################################################################
# Get User Profile Function
################################################################################

get_user_profile() {
    separator
    log "Fetching user profile..."

    PROFILE=$(curl -s -X GET \
        "${API_URL}/api/user/profile" \
        -H "Authorization: Bearer ${TOKEN}" \
        -H 'Content-Type: application/json')

    if echo "$PROFILE" | jq -e '.username' > /dev/null 2>&1; then
        success "User profile retrieved!"

        USERNAME_PROFILE=$(echo "$PROFILE" | jq -r '.username')
        EMAIL=$(echo "$PROFILE" | jq -r '.email')
        NAME=$(echo "$PROFILE" | jq -r '.name')
        ROLES=$(echo "$PROFILE" | jq -r '.roles | join(", ")')

        info "Username: $USERNAME_PROFILE"
        info "Email: $EMAIL"
        info "Name: $NAME"
        info "Roles: $ROLES"
    else
        error "Failed to get user profile"
    fi
}

################################################################################
# Get JWT Claims Function
################################################################################

get_jwt_claims() {
    separator
    log "JWT Token Claims:"
    echo ""

    # Decode JWT payload (2nd part)
    PAYLOAD=$(echo "$TOKEN" | cut -d. -f2)
    DECODED=$(echo "$PAYLOAD" | base64 -d 2>/dev/null || echo "$PAYLOAD" | base64 -D 2>/dev/null)

    echo "$DECODED" | jq '.' | while read -r line; do
        echo -e "${CYAN}$line${NC}"
    done
    echo ""
}

################################################################################
# Test Specific Endpoint Function
################################################################################

test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4

    log "Testing: $description"
    info "Method: $method, Endpoint: $endpoint"

    if [ "$method" = "POST" ]; then
        RESPONSE=$(curl -s -X POST \
            "${API_URL}${endpoint}" \
            -H "Authorization: Bearer ${TOKEN}" \
            -H 'Content-Type: application/json' \
            -d "$data")
    else
        RESPONSE=$(curl -s -X GET \
            "${API_URL}${endpoint}" \
            -H "Authorization: Bearer ${TOKEN}" \
            -H 'Content-Type: application/json')
    fi

    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
        "${API_URL}${endpoint}" \
        -H "Authorization: Bearer ${TOKEN}")

    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
        success "$description - HTTP $HTTP_CODE"
    else
        warning "$description - HTTP $HTTP_CODE"
    fi
}

################################################################################
# Admin Test Function
################################################################################

test_admin_endpoint() {
    separator
    log "Testing Admin Endpoint (requires ADMIN role)..."

    ADMIN_RESPONSE=$(curl -s -X GET \
        "${API_URL}/api/admin/dashboard" \
        -H "Authorization: Bearer ${TOKEN}" \
        -H 'Content-Type: application/json')

    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET \
        "${API_URL}/api/admin/dashboard" \
        -H "Authorization: Bearer ${TOKEN}")

    if [ "$HTTP_CODE" = "200" ]; then
        success "Admin access granted!"
    elif [ "$HTTP_CODE" = "403" ]; then
        warning "Access denied - User role doesn't have ADMIN privileges"
    else
        error "Admin endpoint error - HTTP $HTTP_CODE"
    fi
}

################################################################################
# Generate Report Function
################################################################################

generate_report() {
    separator
    log "Generating test report..."

    REPORT_FILE="${SCRIPT_DIR}/jwt_test_report_$(date +%Y%m%d_%H%M%S).html"

    cat > "$REPORT_FILE" << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>JWT Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }
        .container { max-width: 1200px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; }
        h1 { color: #333; border-bottom: 3px solid #007bff; padding-bottom: 10px; }
        .success { color: #28a745; font-weight: bold; }
        .error { color: #dc3545; font-weight: bold; }
        .info { color: #17a2b8; }
        table { width: 100%; border-collapse: collapse; margin: 20px 0; }
        th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
        th { background-color: #007bff; color: white; }
        tr:nth-child(even) { background-color: #f9f9f9; }
        .timestamp { font-size: 0.9em; color: #666; }
    </style>
</head>
<body>
    <div class="container">
        <h1>🔐 JWT Authentication Test Report</h1>
        <p class="timestamp">Generated: <script>document.write(new Date().toLocaleString());</script></p>

        <h2>Test Summary</h2>
        <table>
            <tr>
                <th>Metric</th>
                <th>Value</th>
            </tr>
            <tr>
                <td>Keycloak URL</td>
                <td>KEYCLOAK_URL</td>
            </tr>
            <tr>
                <td>API URL</td>
                <td>API_URL</td>
            </tr>
            <tr>
                <td>Total Books</td>
                <td>BOOK_COUNT</td>
            </tr>
            <tr>
                <td>Token Expiry</td>
                <td>EXPIRES_IN seconds</td>
            </tr>
        </table>

        <h2>✅ Test Results</h2>
        <p class="success">All tests completed successfully!</p>
    </div>
</body>
</html>
EOF

    success "Report generated: $REPORT_FILE"
}

################################################################################
# Main Workflow
################################################################################

main() {
    clear
    separator
    log "JWT Login & GetBooks Test Script"
    log "=================================="
    info "Keycloak: $KEYCLOAK_URL"
    info "API: $API_URL"
    info "User: $USERNAME"
    info "Log file: $LOG_FILE"

    # Check prerequisites
    check_prerequisites || exit 1

    # Login
    login || exit 1

    # Get books
    get_books || exit 1

    # Display books
    display_books

    # Get user profile
    get_user_profile

    # Get JWT claims
    get_jwt_claims

    # Test other endpoints
    test_endpoint "GET" "/api/books/1" "" "Get Book by ID"
    test_endpoint "GET" "/api/user/profile" "" "Get User Profile"

    # Test admin endpoint
    test_admin_endpoint

    # Summary
    separator
    success "All tests completed!"
    info "Total books: $BOOK_COUNT"
    info "Log saved to: $LOG_FILE"
    info "Token valid for: $EXPIRES_IN seconds"

    separator
    log "Test workflow completed successfully! 🎉"
}

################################################################################
# Script Entry Point
################################################################################

# Show usage
if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    echo "Usage: ./jwt_test.sh [KEYCLOAK_URL] [API_URL]"
    echo ""
    echo "Examples:"
    echo "  ./jwt_test.sh                                            # Use defaults"
    echo "  ./jwt_test.sh http://localhost:8180 http://localhost:8080"
    echo ""
    exit 0
fi

# Run main
main

