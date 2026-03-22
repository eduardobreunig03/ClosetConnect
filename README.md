# ClosetConnect

A peer-to-peer clothing rental platform where users can list, browse, rent, and review clothing items. Built with a Spring Boot backend, Next.js frontend, and PostgreSQL database — all orchestrated with Docker.

---

## What It Does

ClosetConnect lets users:
- List clothing items for rent with photos and descriptions
- Browse and filter available clothing cards
- Send and manage rental requests
- Leave reviews for both clothing items and other users
- Authenticate securely with JWT tokens

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot, Spring Security (JWT) |
| Frontend | Next.js (TypeScript), Tailwind CSS |
| Database | PostgreSQL 15 |
| Dev Tools | Docker, Docker Compose, pgAdmin 4, Gradle |

---

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [Node.js 18+](https://nodejs.org/)
- Java 17+ and Gradle (or use the included `gradlew` wrapper)

---

## How to Run

### 1. Start the database

```bash
cd closetconnect
docker-compose up -d
```

This starts:
- **PostgreSQL** on port `5433`
- **pgAdmin** on port `5050` (admin@closetconnect.com / admin123)

### 2. Start the backend

```bash
cd closetconnect/backend
./gradlew bootRun
```

The API will be available at `http://localhost:8080`.

### 3. Start the frontend

```bash
cd closetconnect/frontend
npm install
npm run dev
```

Open `http://localhost:3000` in your browser.

---

## Features

### Authentication
- Register and log in with JWT-based authentication
- Secured endpoints — only authenticated users can create listings or make requests

### Clothing Cards
- Create listings with title, description, size, category, price, and images
- Filter and search clothing cards by various criteria
- Edit or delete your own listings

### Rentals
- Send rental requests to clothing owners
- Owners can approve or reject requests
- Track active and past rentals

### Reviews
- Leave reviews for clothing items after a rental
- Review other users based on your experience
- View aggregate ratings on profiles and listings

### Admin / Dev Tools
- pgAdmin accessible at `http://localhost:5050` for direct database inspection

---

## Project Structure

```
closetconnect/
├── backend/                  # Spring Boot API
│   └── src/main/java/com/example/closetconnect/
│       ├── controller/       # REST endpoints
│       ├── services/         # Business logic
│       ├── repositories/     # JPA data access
│       ├── entities/         # Database models
│       ├── config/           # Security & app config
│       └── filters/          # JWT filter
├── frontend/                 # Next.js app
│   └── src/                  # Pages, components, styles
├── init-scripts/             # SQL scripts run on DB first boot
├── sql_backup/               # Database backup files
└── docker-compose.yml        # PostgreSQL + pgAdmin services
```
