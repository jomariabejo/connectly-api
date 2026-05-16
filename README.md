# 🌐 Connectly API

A robust, scalable backend for a modern social platform — enabling users to post, comment, and engage with content while ensuring **secure authentication**, **privacy controls**, and **role-based access**. Built with integration and analytics in mind.

![Java](https://img.shields.io/badge/Java-17+-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=flat-square&logo=spring-boot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-336791?style=flat-square&logo=postgresql)
![JWT](https://img.shields.io/badge/JWT-Auth-blue?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)
![GitHub Stars](https://img.shields.io/github/stars/jomariabejo/connectly-api?style=flat-square&logo=github)
![Build Status](https://img.shields.io/badge/Status-Active-success?style=flat-square)

---

## ✨ Features

- 🧑‍🤝‍🧑 User Authentication with email verification
- 🔐 Privacy & Role-based Access Control
- 📝 Post & Comment System with likes
- 📊 Full REST API
- 🔧 Easy local development setup
- 💬 RESTful API Design

---

## 📚 API Testing

All API endpoints are available as `.http` files in the `src/main/resources/docs/http-template/` directory for easy testing with VS Code REST Client extension.

👉 Install [REST Client](https://marketplace.visualstudio.com/items?itemName=humao.rest-client) extension and click "Send Request" on any file.

---

## 🛠️ Tech Stack

- **Backend:** Spring Boot 3
- **Database:** PostgreSQL
- **Authentication:** JWT
- **Email:** MailHog (local development)
- **Build:** Gradle

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+**
- **PostgreSQL** (running locally)
- **Gradle** (included with `./gradlew`)
- **Docker** (optional, for MailHog)

### Quick Setup

#### 1. Clone & Navigate
```bash
git clone https://github.com/jomariabejo/connectly-api.git
cd connectly-api
```

#### 2. Setup PostgreSQL Database
```bash
# Create database
createdb connectly_db

# If using different credentials, update src/main/resources/application.properties
```

#### 3. Setup Local Email with MailHog
For development, we use **MailHog** to capture emails locally:

**Option A: Using Docker** (easiest)
```bash
docker run -d --name mailhog -p 1025:1025 -p 8025:8025 mailpit/mailpit
```

**Option B: Direct Install** (requires Go)
```bash
go install github.com/mailhog/MailHog@latest
MailHog
```

Once running:
- 📧 **SMTP Server:** `localhost:1025` (app sends emails here)
- 🌐 **Web UI:** http://localhost:8025 (view sent emails)

#### 4. Run the Application
```bash
./gradlew bootRun
```

The API will be running at `http://localhost:8080`

---

## 📡 API Endpoints

### Authentication
- `POST /v1/auth/registration` - Register new user
- `POST /v1/auth/login` - Login user
- `GET /v1/auth/verify?token=TOKEN` - Verify email

### Users
- `GET /v1/users/me` - Get current user profile
- `PUT /v1/users/me` - Update profile

### Posts
- `POST /v1/posts` - Create post
- `GET /v1/posts` - Get all posts
- `GET /v1/posts/{id}` - Get single post
- `PUT /v1/posts/{id}` - Update post
- `DELETE /v1/posts/{id}` - Delete post
- `POST /v1/posts/{id}/likes/toggle` - Toggle post like

### Comments
- `POST /v1/posts/{postId}/comments` - Create comment
- `GET /v1/posts/{postId}/comments` - Get post comments
- `PUT /v1/posts/{postId}/comments/{commentId}` - Update comment
- `DELETE /v1/posts/{postId}/comments/{commentId}` - Delete comment

### Orders
- `POST /v1/orders` - Create order
- `GET /v1/orders` - Get orders with filtering and pagination
- `GET /v1/orders/{id}` - Get single order
- `PATCH /v1/orders/{id}/status` - Update order status
- `GET /v1/orders/{id}/status-history` - Get order status history

### Payments
- `POST /v1/payments/checkout` - Create provider checkout session
- `GET /v1/payments/{paymentId}` - Get payment details
- `GET /v1/orders/{orderId}/payments` - Get payments for an order
- `POST /v1/payments/webhooks/{provider}` - Receive provider webhook callbacks

---

## 🧪 Testing API Requests

1. **Install REST Client** extension in VS Code
2. Open any file in `src/main/resources/docs/http-template/`
3. Click "Send Request" button above the request
4. View response in the sidebar

Example: `src/main/resources/docs/http-template/auth/register.http`

---

## 🤝 Contributing

Feel free to submit issues and enhancement requests!
