# SmartFinance PTY - Backend API

> Control financiero personal para Panamá — Spring Boot 3.4.1 + Java 21

---

## 🚀 Stack Tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Spring Boot 3.4.1 + Java 21 |
| Base de datos | PostgreSQL 16 |
| Caché | Redis 7 |
| API | REST + GraphQL (Spring for GraphQL) |
| Autenticación | JWT + Refresh Token |
| Migraciones | Flyway |
| Contenedores | Docker + Docker Compose |
| CI/CD | GitHub Actions |
| Documentación | Swagger / OpenAPI 3 |

---

## 📋 Módulos Implementados

- ✅ **Auth** — Register, Login, Refresh Token, Logout
- ✅ **Finance** — Ingresos con deducciones, Gastos por categoría
- ✅ **Dashboard** — Balance en tiempo real (GraphQL)
- ✅ **Budgets** — Presupuestos con alertas al 80%
- ✅ **Savings Goals** — Metas de ahorro
- ✅ **Notifications** — Push + Email con cron jobs
- ✅ **Analytics** — Tendencias, predicciones, recomendaciones IA
- ✅ **File Upload** — Recibos y facturas (Local/S3/Cloudinary)
- ✅ **Docker** — Multi-stage build, Docker Compose
- ✅ **CI/CD** — GitHub Actions con build, test y deploy
- ✅ **Swagger** — Documentación OpenAPI 3

---

## ⚡ Inicio Rápido

### Prerequisitos
- Java 21
- Maven 3.9+
- Docker y Docker Compose
- PostgreSQL 16 (o usar Docker)

### 1. Clonar y configurar
```bash
git clone https://github.com/tu-usuario/smartfinancepty.git
cd smartfinancepty

# Copiar variables de entorno
cp .env.example .env
# Editar .env con tus valores
```

### 2. Levantar con Docker (recomendado)
```bash
# Solo dependencias (DB + Redis) — backend corre local
docker compose -f docker-compose.dev.yml up -d

# Todo el stack completo
docker compose up -d --build
```

### 3. Levantar solo el backend (desarrollo)
```bash
cd backend
mvn spring-boot:run
```

---

## 📚 Documentación

| URL | Descripción |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Swagger UI interactivo |
| `http://localhost:8080/api-docs` | OpenAPI JSON |
| `http://localhost:8080/graphiql` | GraphQL playground |
| `http://localhost:8080/actuator/health` | Health check |
| `http://localhost:5050` | pgAdmin (solo dev) |

---

## 🔐 Autenticación

```bash
# 1. Register
POST /api/v1/auth/register
{
  "fullName": "Joel Guerrero",
  "email": "joel@smartfinance.com",
  "password": "Password123!"
}

# 2. Login → guarda el accessToken
POST /api/v1/auth/login

# 3. Usar token en cada request
Authorization: Bearer eyJhbGci...

# 4. Renovar token
POST /api/v1/auth/refresh
{ "refreshToken": "uuid-del-refresh-token" }
```

---

## 🔮 GraphQL

```bash
POST /graphql
Authorization: Bearer eyJhbGci...

# Dashboard
query {
  dashboard {
    balance totalNetIncome totalExpensesMonth savingsProjected
    expensesByCategory { categoryName totalAmount percentage }
    recentExpenses { description amount expenseDate }
  }
}

# Analytics
query {
  analytics(year: 2026, month: 5) {
    savingsRate riskLevel riskMessage
    prediction { predictedExpenses confidenceLevel }
    topCategories { categoryName totalAmount trend }
  }
}

# Recomendaciones IA
query {
  recommendations {
    type title message priority potentialSavings
  }
}
```

---

## 🐳 Docker

```bash
# Levantar todo
docker compose up -d --build

# Ver logs
docker compose logs -f backend

# Parar
docker compose down

# Reset completo (borra datos)
docker compose down -v
```

---

## 🧪 Tests

```bash
# Todos los tests
mvn test

# Solo unit tests
mvn test -Dtest="*ServiceTest,JwtServiceTest"

# Con reporte de cobertura
mvn test jacoco:report
# Reporte: target/site/jacoco/index.html
```

---

## 🗄️ Base de Datos

### Migraciones (Flyway)
| Versión | Descripción |
|---|---|
| V1 | Users y Refresh Tokens |
| V2 | Finance (Incomes, Expenses, Categories) |
| V3 | Budgets y Savings Goals |
| V4 | Notifications y Reminders |
| V5 | File Attachments |

### Categorías por defecto
`Hogar, Alimentación, Transporte, Salud, Entretenimiento, Educación, Ropa, Inversiones, Otros`

---

## 📁 Estructura del Proyecto

```
smartfinancepty/
├── backend/
│   ├── src/main/java/com/smartfinancepty/finance/
│   │   ├── config/          # Spring Security, OpenAPI
│   │   ├── controllers/     # REST Controllers
│   │   ├── domain/          # JPA Entities
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── exception/       # Exception Handlers
│   │   ├── graphql/         # GraphQL Resolvers y DTOs
│   │   ├── jobs/            # Cron Jobs (NotificationScheduler)
│   │   ├── notification/    # Email y Push Services
│   │   ├── repository/      # JPA Repositories
│   │   ├── security/        # JWT, Filters
│   │   └── service/         # Business Logic
│   │       ├── analytics/   # Dashboard, Analytics
│   │       ├── auth/        # AuthService
│   │       ├── finance/     # Income, Expense, Budget
│   │       ├── notification/# NotificationService
│   │       ├── storage/     # Local, S3, Cloudinary
│   │       └── upload/      # FileAttachmentService
│   └── src/main/resources/
│       ├── db/migration/    # Flyway SQL
│       └── graphql/         # .graphqls schemas
├── .github/workflows/       # CI/CD GitHub Actions
├── docker-compose.yml       # Stack completo
├── docker-compose.dev.yml   # Solo dependencias
└── .env.example
```

---

## 🌱 Variables de Entorno

```env
POSTGRES_DB=smartfinance_db
POSTGRES_USER=smartfinance
POSTGRES_PASSWORD=tu_password_seguro

REDIS_PASSWORD=tu_redis_password

JWT_SECRET=tu_clave_256_bits_minimo

SPRING_PROFILES_ACTIVE=prod

# Para S3 (prod)
AWS_ACCESS_KEY=...
AWS_SECRET_KEY=...

# Para Cloudinary (prod)
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
```

---

## 👨‍💻 Desarrollado por

**Joel Guerrero** — SmartFinance PTY  
📧 joelg1014@hotmail.com  
🇵🇦 Panamá
