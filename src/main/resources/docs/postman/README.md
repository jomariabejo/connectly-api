# Connectly API v1 - Postman Collection

**Complete, organized, and tested Postman collection for the Connectly API with 115+ endpoints across 17 API modules.**

---

## 📋 Quick Start

### 1. Import the Collection
- **File**: `connectly-api-v1.postman_collection.json`
- Open Postman → Collections → Import → Select this file
- All 17 API groups and 115 endpoints will be imported

### 2. Set Environment URLs
The collection supports multiple deployment environments:
- **Local Development**: `http://localhost:8080/api` (default)
- **Stage**: `http://localhost:8081/api` (set `baseUrl` to `{{baseUrlStage}}`)
- **Demo**: `http://localhost:8082/api` (set `baseUrl` to `{{baseUrlDemo}}`)
- **Production**: `http://localhost:8083/api` (set `baseUrl` to `{{baseUrlProd}}`)

### 3. Authenticate
1. Go to **Authentication API** folder
2. Run **Login User** request with test credentials:
   - Email: `user@example.com`
   - Password: `Password123!`
3. Token is automatically extracted and stored in `userToken` collection variable
4. All authenticated endpoints now use this token

---

## 📚 Collection Structure

### API Groups (17 modules, 115 endpoints)

| Module | Endpoints | Description |
|--------|:---------:|-------------|
| **Authentication API** | 10 | Registration, login, email verification, password reset |
| **User Management API** | 12 | User profiles, settings, account administration |
| **Posts API** | 9 | Post creation, retrieval, feed management |
| **Comments API** | 5 | Comment CRUD on posts |
| **Post Likes API** | 3 | Like/unlike posts, like counts |
| **Follow System API** | 10 | Following, followers, follow requests |
| **Circles API** | 9 | Create close groups for content sharing |
| **Block Management API** | 8 | Block/unblock users, block management |
| **Orders API** | 5 | Order creation, status tracking |
| **Payments API** | 4 | Payment checkout, payment management |
| **Inventory API** | 5 | Inventory management and adjustments |
| **Ticketing API** | 6 | Support ticket management |
| **Workforce API** | 5 | Employee scheduling, time tracking |
| **Payroll API** | 5 | Payroll management and processing |
| **CRM API** | 9 | Customer management with notes/interactions |
| **Tenant Management API** | 5 | Workspace creation and invitations |
| **Public & System APIs** | 5 | Health checks, test endpoints |

---

## 🔑 Collection Variables (37 total)

### Environment URLs
```
baseUrl              → Default: http://localhost:8080/api
baseUrlStage         → Stage: http://localhost:8081/api
baseUrlDemo          → Demo: http://localhost:8082/api
baseUrlProd          → Production: http://localhost:8083/api
```

### Authentication Tokens
```
userToken            → JWT token for authenticated requests (auto-filled after login)
adminToken           → Admin JWT token (if different auth flow needed)
verificationToken    → Email verification token (copy from Mailpit)
reactivationToken    → Account reactivation token
resetToken           → Password reset token
resetOtp             → OTP for password reset
```

### User & Credentials
```
userEmail            → Test user email (default: user@example.com)
userPassword         → Test user password (default: Password123!)
username             → Test username
adminEmail           → Admin email
adminPassword        → Admin password
```

### Entity IDs (auto-populated by creating/fetching)
```
currentUserId        → Current authenticated user's ID
targetUserId         → ID of another user for operations
postId               → Post ID for comments/likes
commentId            → Comment ID
circleId             → Circle ID
memberId             → Circle member ID
blockId              → Block ID
followId             → Follow request ID
orderId              → Order ID
paymentId            → Payment ID
customerId           → CRM customer ID
employeeId           → Payroll employee ID
payrollRunId         → Payroll run ID
ticketId             → Support ticket ID
inventorySku         → Inventory product SKU
tenantId             → Workspace/tenant ID
```

### Enums & Status Values
```
orderStatus          → Order status (e.g., PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED)
ticketStatus         → Ticket status (e.g., OPEN, IN_PROGRESS, RESOLVED, CLOSED)
ticketPriority       → Ticket priority (e.g., LOW, MEDIUM, HIGH, URGENT)
inviteToken          → Workspace invitation token
adminDashboardToken  → Admin dashboard access token
```

---

## 🔐 Authentication Flows

### Standard User Flow (Recommended)
1. **Register User** (`POST /auth/registration`)
   - Creates new user account
   - Sends verification email
   
2. **Verify Email** (`GET /auth/verify?token={{verificationToken}}`)
   - Copy token from Mailpit (if using local mail)
   - Verifies user email
   
3. **Login User** (`POST /auth/login`)
   - Authenticates user
   - Returns JWT token (auto-saved to `userToken`)
   
4. All subsequent authenticated requests use `{{userToken}}`

### Alternative: Customer Registration Flow
- Use **Register Customer** (`POST /auth/register/customer`) for business/customer accounts
- Same verification flow applies

### Password Reset Flow
1. **Forgot Password - Email** (`POST /auth/forgot-password/email`)
2. Extract token from email (via Mailpit)
3. **Reset Password** (`POST /auth/reset-password`) with token and new password

---

## 📝 Common Workflows

### User Account Setup (5-10 minutes)
1. Register User (in Authentication API)
2. Verify Email (get token from Mailpit: `http://localhost:8025`)
3. Login User
4. Get Current User Profile
5. Update Account Privacy/Settings

### Create & Share Content (10-15 minutes)
1. Login User
2. Create Post (in Posts API)
3. Create Comment on that post
4. Like the post
5. Create Circle (in Circles API)
6. Add members to circle

### Follow & Manage Network (10-15 minutes)
1. Create/login as User A
2. Create/login as User B (different token in `{{userToken}}`)
3. Follow User B from User A
4. List followers
5. Check if following
6. Unfollow if needed

### Order & Payment Flow (15-20 minutes)
1. Create Order (in Orders API)
2. Create Payment Checkout
3. Get Order Status
4. Update Order Status
5. View Order Payments

### Admin Tasks (10-15 minutes)
1. Login as admin user
2. List Users Paginated
3. List Users Scheduled for Deletion
4. Access Admin Dashboard

---

## 🧪 Testing & Assertions

### Built-in tests on every endpoint
- **GET requests**: Validate `200` status code
- **POST requests**: Validate `200` or `201` status codes
- **PUT/PATCH requests**: Validate `200` or `201` status codes
- **DELETE requests**: Validate `200` or `204` status codes

### Auto-extraction of IDs
- Login request auto-extracts `userToken`
- Create requests auto-extract entity IDs (postId, orderId, etc.)
- Check response bodies in **Tests** tab to see saved variables

### Run in Postman
1. Select a folder (e.g., "Authentication API")
2. Click **Run** (play icon + arrow)
3. Select requests to run in sequence
4. View test results and responses

---

## 🌐 Public Endpoints

These endpoints require **no authentication**:

```
GET  /api/v1/public/hello               → Health check
GET  /api/v1/public/tenants/{slug}      → Get public tenant info
GET  /api/v1/test/helloworld            → Test endpoint
POST /api/v1/payments/webhooks/{provider} → Payment webhooks
```

Use `noauth` in Postman for these requests (already configured).

---

## 📬 Email Verification (Local Development)

If using Docker Compose with Mailpit:

1. **Mailpit UI**: `http://localhost:8025`
2. After registration, check Mailpit inbox
3. Copy verification token from email
4. Paste into `verificationToken` collection variable
5. Run Verify Email request

---

## ✅ Verification Checklist

After importing, verify:

- [ ] Collection imports without errors
- [ ] All 17 folders visible
- [ ] 115+ requests loaded
- [ ] Variables tab shows 37+ variables
- [ ] `baseUrl` set to your deployment
- [ ] Login request executes successfully
- [ ] `userToken` is populated after login
- [ ] GET requests return 200 status
- [ ] POST requests return 201 or 200 status

---

## 🔄 Migration from Old Collection

If upgrading from the previous 3-folder collection:

1. **Backup old collection**: Already saved as `connectly-api-v1.postman_collection.json.bak`
2. **Delete in Postman**: Remove old "Connectly API v1" collection
3. **Import new**: Import this comprehensive version
4. **Update credentials**: Set `userEmail`, `userPassword`, `baseUrl` as needed
5. **Re-authenticate**: Run login request to get new `userToken`

---

## 📊 Statistics

- **Total API Folders**: 17
- **Total Endpoints**: 115
- **HTTP Methods**: GET (58), POST (37), DELETE (9), PUT (8), PATCH (3)
- **Authentication Types**: 108 Bearer Token, 7 Public/No Auth
- **Collection Variables**: 37
- **Built-in Tests**: Every endpoint

---

## 🐛 Troubleshooting

### "Token is invalid or expired"
- Run Login User request again
- Token is auto-saved to `userToken`

### "targetUserId not found"
- Create another user with different email
- Set `targetUserId` collection variable to that user's ID

### "postId not found"
- Create a Post first (in Posts API)
- postId is auto-saved from create response

### "Cannot verify email"
- Check Mailpit at `http://localhost:8025`
- Copy verification token and paste into `verificationToken` variable
- Wait 5-10 seconds for email to arrive

### "500 Internal Server Error"
- Check your backend logs: `docker logs connectly-api`
- Ensure Docker containers are running: `docker compose ps`
- Check database migrations ran successfully

---

## 📚 Additional Resources

- **API Documentation**: See `docs/` folder for detailed endpoint specs
- **HTTP Examples**: `docs/http-template/` has individual endpoint examples
- **Curl Examples**: `docs/curls/` has curl commands
- **Backend Code**: `src/main/java/com/jomariabejo/connectly_api/` controllers
- **Database Schema**: `src/main/resources/schema.sql`

---

## 📝 Notes

- **Request Bodies**: All sample bodies use example values - adjust as needed
- **Pagination**: Default page=0, size=10 - adjust parameters in GET requests
- **Timestamps**: Responses include createdDate, updatedDate fields
- **Soft Deletes**: User deletion has 30-day grace period before permanent removal
- **Multi-tenant**: Some endpoints support tenant context (headers/params)

---

**Collection Version**: v1.0  
**Last Updated**: 2026-05-23  
**Status**: ✅ Complete & Ready to Use

