#!/bin/bash

# Keycloak Setup Script for Books API
# This script configures Keycloak realm, client, roles, and test users

echo "================================================"
echo "Keycloak Setup for Books API"
echo "================================================"

KEYCLOAK_URL="http://localhost:8180"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin"
REALM_NAME="books-realm"
CLIENT_ID="books-service"
CLIENT_SECRET="books-secret-2024"

echo ""
echo "Waiting for Keycloak to be ready..."
until curl -sf "${KEYCLOAK_URL}/health/ready" > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo ""
echo "✓ Keycloak is ready!"

# Get admin access token
echo ""
echo "Getting admin access token..."
ADMIN_TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" == "null" ]; then
    echo "✗ Failed to get admin token"
    exit 1
fi
echo "✓ Admin token obtained"

# Create realm
echo ""
echo "Creating realm: ${REALM_NAME}..."
REALM_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${KEYCLOAK_URL}/admin/realms" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "realm": "'${REALM_NAME}'",
    "enabled": true,
    "displayName": "Books Realm",
    "accessTokenLifespan": 3600,
    "sslRequired": "none"
  }')

HTTP_CODE=$(echo "$REALM_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" == "201" ]; then
    echo "✓ Realm created successfully"
elif [ "$HTTP_CODE" == "409" ]; then
    echo "! Realm already exists"
else
    echo "✗ Failed to create realm (HTTP ${HTTP_CODE})"
fi

# Create roles
echo ""
echo "Creating roles..."
for ROLE in USER ADMIN; do
    ROLE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/roles" \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" \
      -H "Content-Type: application/json" \
      -d '{
        "name": "'${ROLE}'",
        "description": "'${ROLE}' role for Books API"
      }')

    HTTP_CODE=$(echo "$ROLE_RESPONSE" | tail -n1)
    if [ "$HTTP_CODE" == "201" ]; then
        echo "  ✓ Role ${ROLE} created"
    elif [ "$HTTP_CODE" == "409" ]; then
        echo "  ! Role ${ROLE} already exists"
    else
        echo "  ✗ Failed to create role ${ROLE}"
    fi
done

# Create client
echo ""
echo "Creating client: ${CLIENT_ID}..."
CLIENT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/clients" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "'${CLIENT_ID}'",
    "name": "Books Service Client",
    "description": "OAuth2 client for Books API",
    "enabled": true,
    "clientAuthenticatorType": "client-secret",
    "secret": "'${CLIENT_SECRET}'",
    "redirectUris": ["http://localhost:8080/*", "http://localhost:3000/*"],
    "webOrigins": ["http://localhost:8080", "http://localhost:3000"],
    "standardFlowEnabled": true,
    "directAccessGrantsEnabled": true,
    "serviceAccountsEnabled": true,
    "authorizationServicesEnabled": false,
    "publicClient": false,
    "protocol": "openid-connect",
    "attributes": {
      "access.token.lifespan": "3600"
    }
  }')

HTTP_CODE=$(echo "$CLIENT_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" == "201" ]; then
    echo "✓ Client created successfully"
elif [ "$HTTP_CODE" == "409" ]; then
    echo "! Client already exists"
else
    echo "✗ Failed to create client (HTTP ${HTTP_CODE})"
fi

# Get client UUID
CLIENT_UUID=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/clients?clientId=${CLIENT_ID}" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" | jq -r '.[0].id')

# Create test users
echo ""
echo "Creating test users..."

# Create admin user
ADMIN_USER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "email": "admin@books.com",
    "firstName": "Admin",
    "lastName": "User",
    "enabled": true,
    "emailVerified": true,
    "credentials": [{
      "type": "password",
      "value": "admin123",
      "temporary": false
    }]
  }')

HTTP_CODE=$(echo "$ADMIN_USER_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" == "201" ]; then
    echo "  ✓ Admin user created"

    # Get admin user ID and assign ADMIN role
    ADMIN_USER_ID=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users?username=admin" \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" | jq -r '.[0].id')

    ADMIN_ROLE_ID=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/roles/ADMIN" \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" | jq -r '.id')

    curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users/${ADMIN_USER_ID}/role-mappings/realm" \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" \
      -H "Content-Type: application/json" \
      -d '[{
        "id": "'${ADMIN_ROLE_ID}'",
        "name": "ADMIN"
      }]'
    echo "    ✓ ADMIN role assigned"
elif [ "$HTTP_CODE" == "409" ]; then
    echo "  ! Admin user already exists"
fi

# Create regular user
USER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user",
    "email": "user@books.com",
    "firstName": "Regular",
    "lastName": "User",
    "enabled": true,
    "emailVerified": true,
    "credentials": [{
      "type": "password",
      "value": "user123",
      "temporary": false
    }]
  }')

HTTP_CODE=$(echo "$USER_RESPONSE" | tail -n1)
if [ "$HTTP_CODE" == "201" ]; then
    echo "  ✓ Regular user created"

    # Get user ID and assign USER role
    USER_ID=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users?username=user" \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" | jq -r '.[0].id')

    USER_ROLE_ID=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/roles/USER" \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" | jq -r '.id')

    curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/users/${USER_ID}/role-mappings/realm" \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" \
      -H "Content-Type: application/json" \
      -d '[{
        "id": "'${USER_ROLE_ID}'",
        "name": "USER"
      }]'
    echo "    ✓ USER role assigned"
elif [ "$HTTP_CODE" == "409" ]; then
    echo "  ! Regular user already exists"
fi

echo ""
echo "================================================"
echo "✓ Keycloak Setup Complete!"
echo "================================================"
echo ""
echo "Keycloak Admin Console: ${KEYCLOAK_URL}"
echo "  Username: ${ADMIN_USER}"
echo "  Password: ${ADMIN_PASSWORD}"
echo ""
echo "Realm: ${REALM_NAME}"
echo "Client ID: ${CLIENT_ID}"
echo "Client Secret: ${CLIENT_SECRET}"
echo ""
echo "Test Users:"
echo "  Admin User:"
echo "    Username: admin"
echo "    Password: admin123"
echo "    Roles: ADMIN"
echo ""
echo "  Regular User:"
echo "    Username: user"
echo "    Password: user123"
echo "    Roles: USER"
echo ""
echo "Update your application.yml with:"
echo "  spring.security.oauth2.client.registration.keycloak.client-secret: ${CLIENT_SECRET}"
echo ""
echo "================================================"

