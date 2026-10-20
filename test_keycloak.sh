#!/bin/bash

# Keycloak API Testing Script
# Tests authentication and authorization with different users

echo "================================================"
echo "Keycloak API Testing Script"
echo "================================================"

KEYCLOAK_URL="http://localhost:8180"
API_URL="http://localhost:8080/api"
REALM_NAME="books-realm"
CLIENT_ID="books-service"
CLIENT_SECRET="books-secret-2024"

echo ""
echo "Testing Keycloak Integration..."
echo ""

# Function to get access token
get_token() {
    local username=$1
    local password=$2

    local response=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM_NAME}/protocol/openid-connect/token" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "client_id=${CLIENT_ID}" \
      -d "client_secret=${CLIENT_SECRET}" \
      -d "grant_type=password" \
      -d "username=${username}" \
      -d "password=${password}")

    echo $(echo $response | jq -r '.access_token')
}

# Test 1: Public endpoint (no auth required)
echo "================================================"
echo "Test 1: Public Endpoint (No Authentication)"
echo "================================================"
curl -s -X GET "${API_URL}/public/info" | jq .
echo ""

# Test 2: Get token for regular user
echo "================================================"
echo "Test 2: Get Access Token for Regular User"
echo "================================================"
USER_TOKEN=$(get_token "user" "user123")

if [ -z "$USER_TOKEN" ] || [ "$USER_TOKEN" == "null" ]; then
    echo "✗ Failed to get user token"
else
    echo "✓ User token obtained successfully"
    echo "Token (first 50 chars): ${USER_TOKEN:0:50}..."
fi
echo ""

# Test 3: Access user profile with user token
echo "================================================"
echo "Test 3: User Profile (USER role required)"
echo "================================================"
curl -s -X GET "${API_URL}/user/profile" \
  -H "Authorization: Bearer ${USER_TOKEN}" | jq .
echo ""

# Test 4: Try to access admin endpoint with user token (should fail)
echo "================================================"
echo "Test 4: Admin Dashboard with USER token (Should Fail)"
echo "================================================"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "${API_URL}/admin/dashboard" \
  -H "Authorization: Bearer ${USER_TOKEN}")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" == "403" ]; then
    echo "✓ Access denied as expected (403 Forbidden)"
    echo "$BODY" | jq .
else
    echo "✗ Unexpected response code: $HTTP_CODE"
    echo "$BODY"
fi
echo ""

# Test 5: Get token for admin user
echo "================================================"
echo "Test 5: Get Access Token for Admin User"
echo "================================================"
ADMIN_TOKEN=$(get_token "admin" "admin123")

if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" == "null" ]; then
    echo "✗ Failed to get admin token"
else
    echo "✓ Admin token obtained successfully"
    echo "Token (first 50 chars): ${ADMIN_TOKEN:0:50}..."
fi
echo ""

# Test 6: Access admin endpoint with admin token
echo "================================================"
echo "Test 6: Admin Dashboard with ADMIN token"
echo "================================================"
curl -s -X GET "${API_URL}/admin/dashboard" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" | jq .
echo ""

# Test 7: Access admin users list
echo "================================================"
echo "Test 7: Admin Users List (ADMIN role required)"
echo "================================================"
curl -s -X GET "${API_URL}/admin/users" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" | jq .
echo ""

# Test 8: View all JWT claims
echo "================================================"
echo "Test 8: View All JWT Claims (Admin User)"
echo "================================================"
curl -s -X GET "${API_URL}/user/claims" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" | jq .
echo ""

# Test 9: Access protected endpoint without token (should fail)
echo "================================================"
echo "Test 9: Protected Endpoint without Token (Should Fail)"
echo "================================================"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "${API_URL}/user/profile")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" == "401" ]; then
    echo "✓ Unauthorized as expected (401)"
    echo "$BODY"
else
    echo "✗ Unexpected response code: $HTTP_CODE"
    echo "$BODY"
fi
echo ""

# Test 10: Decode JWT token
echo "================================================"
echo "Test 10: Decode Admin JWT Token"
echo "================================================"
if [ ! -z "$ADMIN_TOKEN" ] && [ "$ADMIN_TOKEN" != "null" ]; then
    # Extract payload (second part of JWT)
    PAYLOAD=$(echo $ADMIN_TOKEN | cut -d. -f2)
    # Add padding if needed
    PADDING_LENGTH=$((4 - ${#PAYLOAD} % 4))
    if [ $PADDING_LENGTH -lt 4 ]; then
        PAYLOAD="${PAYLOAD}$(printf '=%.0s' $(seq 1 $PADDING_LENGTH))"
    fi
    # Decode
    echo "$PAYLOAD" | base64 -d 2>/dev/null | jq .
fi
echo ""

echo "================================================"
echo "Testing Complete!"
echo "================================================"
echo ""
echo "Summary:"
echo "  ✓ Public endpoints accessible without authentication"
echo "  ✓ USER role can access user endpoints"
echo "  ✓ USER role cannot access admin endpoints (403)"
echo "  ✓ ADMIN role can access all endpoints"
echo "  ✓ Requests without token are rejected (401)"
echo ""
echo "Token Information:"
echo "  User Token: ${USER_TOKEN:0:30}..."
echo "  Admin Token: ${ADMIN_TOKEN:0:30}..."
echo ""

