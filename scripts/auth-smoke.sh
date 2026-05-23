#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8082/api}"
MAILPIT_URL="${MAILPIT_URL:-http://localhost:8025}"
DEFAULT_PASSWORD="${DEFAULT_PASSWORD:-Password123!}"
NEW_PASSWORD_PREFIX="${NEW_PASSWORD_PREFIX:-NewSecurePassword123!}"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

log() {
  printf '%s\n' "$*"
}

fail() {
  printf 'FAIL: %s\n' "$*" >&2
  exit 1
}

pass() {
  printf 'PASS: %s\n' "$*"
}

require_cmd curl
require_cmd jq

HTTP_STATUS=""
HTTP_BODY=""

http_json() {
  local method="$1"
  local url="$2"
  local data="${3:-}"
  local response
  if [[ -n "$data" ]]; then
    response=$(curl -sS -X "$method" "$url" \
      -H "Content-Type: application/json" \
      -d "$data" \
      -w $'\n%{http_code}')
  else
    response=$(curl -sS -X "$method" "$url" \
      -H "Content-Type: application/json" \
      -w $'\n%{http_code}')
  fi
  HTTP_STATUS="${response##*$'\n'}"
  HTTP_BODY="${response%$'\n'*}"
}

assert_status() {
  local expected="$1"
  local label="$2"
  if [[ "$HTTP_STATUS" != "$expected" ]]; then
    printf 'FAIL: %s (status %s)\n' "$label" "$HTTP_STATUS" >&2
    printf '%s\n' "$HTTP_BODY" >&2
    exit 1
  fi
}

assert_json_field() {
  local jq_expr="$1"
  local label="$2"
  local value
  value=$(printf '%s' "$HTTP_BODY" | jq -er "$jq_expr") || {
    printf 'FAIL: %s (json assertion failed)\n' "$label" >&2
    printf '%s\n' "$HTTP_BODY" >&2
    exit 1
  }
}

assert_text_contains() {
  local needle="$1"
  local label="$2"
  if [[ "$HTTP_BODY" != *"$needle"* ]]; then
    printf 'FAIL: %s (missing text)\n' "$label" >&2
    printf '%s\n' "$HTTP_BODY" >&2
    exit 1
  fi
}

health_check() {
  http_json GET "$BASE_URL/actuator/health"
  assert_status 200 "health check"
  assert_json_field '.status == "UP"' "health check status"
  pass "health check"
}

unique_suffix() {
  date +%s%N | tail -c 10
}

mailpit_message_id() {
  local email="$1"
  local subject="$2"
  curl -sS "$MAILPIT_URL/api/v1/messages" | jq -r --arg email "$email" --arg subject "$subject" '
    [
      .messages[]
      | select(.Subject == $subject)
      | select(any(.To[]?; .Address == $email))
    ]
    | sort_by(.Created // 0)
    | last
    | .ID // empty
  '
}

mailpit_message_text() {
  local message_id="$1"
  curl -sS "$MAILPIT_URL/api/v1/message/$message_id" | jq -r '.Text // ""'
}

wait_for_message_text() {
  local email="$1"
  local subject="$2"
  local tries=20
  local sleep_seconds=2
  local message_id=""

  for _ in $(seq 1 "$tries"); do
    message_id=$(mailpit_message_id "$email" "$subject")
    if [[ -n "$message_id" ]]; then
      mailpit_message_text "$message_id"
      return 0
    fi
    sleep "$sleep_seconds"
  done

  fail "mail not found for $email ($subject)"
}

extract_token_from_text() {
  local text="$1"
  printf '%s' "$text" | grep -oE 'token=[A-Za-z0-9-]+' | head -n1 | cut -d= -f2
}

extract_otp_from_text() {
  local text="$1"
  printf '%s' "$text" | grep -oE '[0-9]{6}' | head -n1
}

register_user() {
  local email="$1"
  local username="$2"
  http_json POST "$BASE_URL/v1/auth/registration" \
    "{\"email\":\"$email\",\"password\":\"$DEFAULT_PASSWORD\",\"username\":\"$username\",\"firstName\":\"Jane\",\"lastName\":\"Doe\"}"
  assert_status 200 "register user"
  assert_json_field ".email == \"$email\"" "register response email"
  assert_json_field ".username == \"$username\" or .username == \"$email\"" "register response username"
  assert_json_field ".enabled == false" "register response enabled=false"
  pass "register user ($email)"
}

resend_verification() {
  local email="$1"
  http_json POST "$BASE_URL/v1/auth/verify/resend" \
    "{\"email\":\"$email\"}"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    assert_json_field '.message | length > 0' "resend verification message"
    pass "resend verification"
    return
  fi
  if [[ "$HTTP_STATUS" == "429" ]]; then
    assert_json_field '.message | length > 0' "resend verification throttled"
    pass "resend verification (throttled)"
    return
  fi
  fail "resend verification (status $HTTP_STATUS)"
}

verify_email_token() {
  local token="$1"
  http_json GET "$BASE_URL/v1/auth/verify?token=$token"
  assert_status 200 "verify email"
  assert_text_contains "Email verified successfully" "verify email response"
  pass "verify email"
}

login_user() {
  local email="$1"
  local password="$2"
  http_json POST "$BASE_URL/v1/auth/login" \
    "{\"email\":\"$email\",\"password\":\"$password\"}"
  assert_status 200 "login"
  assert_json_field '.token | length > 10' "login token"
  assert_json_field '.expiresIn > 0' "login expiresIn"
  pass "login user ($email)"
}

forgot_password_email() {
  local email="$1"
  http_json POST "$BASE_URL/v1/auth/forgot-password/email" \
    "{\"email\":\"$email\"}"
  assert_status 200 "forgot password email"
  assert_json_field '.message | length > 0' "forgot password email message"
  pass "forgot password email"
}

forgot_password_otp() {
  local email="$1"
  http_json POST "$BASE_URL/v1/auth/forgot-password/otp" \
    "{\"email\":\"$email\"}"
  assert_status 200 "forgot password otp"
  assert_json_field '.message | length > 0' "forgot password otp message"
  pass "forgot password otp"
}

reset_password_with_token() {
  local token="$1"
  local new_password="$2"
  http_json POST "$BASE_URL/v1/auth/reset-password" \
    "{\"token\":\"$token\",\"otp\":null,\"newPassword\":\"$new_password\"}"
  assert_status 200 "reset password with token"
  assert_json_field '.message | length > 0' "reset password token message"
  pass "reset password with token"
}

reset_password_with_otp() {
  local otp="$1"
  local new_password="$2"
  http_json POST "$BASE_URL/v1/auth/reset-password" \
    "{\"token\":null,\"otp\":\"$otp\",\"newPassword\":\"$new_password\"}"
  assert_status 200 "reset password with otp"
  assert_json_field '.message | length > 0' "reset password otp message"
  pass "reset password with otp"
}

main() {
  health_check

  local suffix
  suffix=$(unique_suffix)

  local email_verify="verify.$suffix@example.com"
  local user_verify="connectly_verify_$suffix"

  local email_reset="reset.$suffix@example.com"
  local user_reset="connectly_reset_$suffix"

  register_user "$email_verify" "$user_verify"
  resend_verification "$email_verify"

  local verification_text
  verification_text=$(wait_for_message_text "$email_verify" "Complete your Connectly registration")
  local verification_token
  verification_token=$(extract_token_from_text "$verification_text")
  if [[ -z "$verification_token" ]]; then
    fail "verification token not found in email"
  fi
  verify_email_token "$verification_token"

  login_user "$email_verify" "$DEFAULT_PASSWORD"

  register_user "$email_reset" "$user_reset"
  local reset_verify_text
  reset_verify_text=$(wait_for_message_text "$email_reset" "Complete your Connectly registration")
  local reset_verify_token
  reset_verify_token=$(extract_token_from_text "$reset_verify_text")
  if [[ -z "$reset_verify_token" ]]; then
    fail "reset user verification token not found in email"
  fi
  verify_email_token "$reset_verify_token"
  local reset_new_password="${NEW_PASSWORD_PREFIX}${suffix}"
  forgot_password_email "$email_reset"
  local reset_text
  reset_text=$(wait_for_message_text "$email_reset" "Reset your Connectly password")
  local reset_token
  reset_token=$(extract_token_from_text "$reset_text")
  if [[ -z "$reset_token" ]]; then
    fail "reset token not found in email"
  fi
  reset_password_with_token "$reset_token" "$reset_new_password"
  login_user "$email_reset" "$reset_new_password"

  local otp_password="${NEW_PASSWORD_PREFIX}Otp${suffix}"
  forgot_password_otp "$email_reset"
  local otp_text
  otp_text=$(wait_for_message_text "$email_reset" "Your Connectly password reset code")
  local reset_otp
  reset_otp=$(extract_otp_from_text "$otp_text")
  if [[ -z "$reset_otp" ]]; then
    fail "reset otp not found in email"
  fi
  reset_password_with_otp "$reset_otp" "$otp_password"
  login_user "$email_reset" "$otp_password"

  pass "all auth flows completed"
}

main "$@"
