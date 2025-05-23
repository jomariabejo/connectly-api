# 🌐 Connectly API

A robust, scalable backend for a modern social platform — enabling users to post, comment, and engage with content while ensuring **secure authentication**, **privacy controls**, and **role-based access**. Built with integration and analytics in mind.

![GitHub Repo Stars](https://img.shields.io/github/stars/jomariabejo/connectly-api?style=flat-square)
![GitHub License](https://img.shields.io/github/license/jomariabejo/connectly-api?style=flat-square)
![Build Status](https://img.shields.io/github/actions/workflow/status/jomariabejo/connectly-api/ci.yml?style=flat-square)

---

## ✨ Features

- 🧑‍🤝‍🧑 User Authentication (incl. Google OAuth)
- 🔐 Privacy & Role-based Access Control
- 📝 Post & Comment System
- 📊 Engagement Analytics
- 🔌 Third-party Integration Ready
- 📈 Scalable Architecture
- 💬 RESTful API with OpenAPI Docs

---

## 📚 API Documentation

👉 View the full API spec on [Bump.sh](https://bump.sh/jomariabejo/doc/connectly-api/)

---

## 🛠️ Tech Stack

- **Backend:** Spring Boot, Mailgun
- **Database:** PostgreSQL
- **Authentication:** JWT + Google OAuth
- **CI/CD:** GitHub Actions, AWS
- **Documentation:** OpenAPI, Bump.sh

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- PostgreSQL
- Gradle

### Setup

```bash
# Clone the repo
git clone https://github.com/jomariabejo/connectly-api.git

# Navigate into the directory
cd connectly-api

# Setup environment variables
cp .env.example .env

# Run the project
./gradlew bootRun
