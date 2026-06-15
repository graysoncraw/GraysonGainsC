# GraysonGains Backend

Spring Boot backend for GraysonGains. This API owns the persistent user data for profiles, personal records, workout cycles, and cycle progression.

## What this backend does

- Verifies Firebase ID tokens on every protected request.
- Uses the verified Firebase UID as the account key for backend records.
- Stores and serves user profile data, personal records, and workout cycle data.
- Calculates prescribed workouts and cycle progression.
- Runs stateless auth with Spring Security.

## Local development

From `ggains-backend/`:

```bash
./mvnw spring-boot:run
```

Run tests with:

```bash
./mvnw test
```

The backend starts on `http://localhost:8080`.

## Required configuration

`src/main/resources/application.properties` expects a local PostgreSQL database:

- database: `graysongains`
- host: `localhost`
- port: `5432`

The default app configuration also expects a Firebase service account file at:

```text
src/main/resources/firebase-service-account.json
```

If that file is missing, Firebase Admin initialization will fail.

## Security model

Every backend request is protected by Spring Security. The flow is:

1. The frontend sends a Firebase ID token in `Authorization: Bearer ...`.
2. `FirebaseAuthenticationFilter` verifies the token with Firebase Admin.
3. The verified Firebase UID is placed into the Spring Security context.
4. `AuthenticatedUserGuard` compares that verified UID against the `firebaseUid` in the route.
5. Controllers only serve or mutate data for the signed-in user.

This backend does not rely on server sessions. It is stateless.

## API groups

### Users

`/api/users`

- create backend user profile
- fetch user by Firebase UID
- fetch user by email
- update user
- delete user

### Personal records

`/api/users/{firebaseUid}/personal-record`

- create the PR record
- fetch the PR record
- update the full PR record
- update a single lift

### Workout cycles

`/api/users/{firebaseUid}/cycles`

- create the first cycle
- fetch the active cycle
- fetch cycle history
- fetch the prescribed workout for a date
- update the active cycle
- progress to the next cycle

## Implementation notes

- `FirebaseConfig` loads `firebase-service-account.json` and initializes Firebase Admin.
- `FirebaseAuthenticationFilter` handles bearer token verification before controller access.
- Validation is handled with Jakarta Bean Validation on request DTOs and entities.
- PostgreSQL and Hibernate are configured for local development with schema auto-update enabled.
