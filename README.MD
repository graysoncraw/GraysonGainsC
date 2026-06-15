# GraysonGains

GraysonGains is a lifting tracker built around Firebase authentication, an Angular frontend, and a Spring backend. The app is organized around a simple onboarding flow: sign in, complete your profile, enter personal records, set up your training cycle, and then use the main app to track training and progression.

## What the app does

- Lets athletes sign up or log in with Firebase.
- Creates and manages a backend user profile tied to the Firebase UID.
- Collects personal records for bench, squat, deadlift, and shoulder press.
- Builds workout cycles and shows prescribed training for the current day.
- Lets users inspect cycle history, progress to the next cycle, and update their training setup.
- Includes a settings area for session troubleshooting and user lookup.

## How it works

The frontend sends Firebase ID tokens to the backend as bearer tokens. The backend verifies those tokens with Firebase Admin, extracts the verified Firebase UID, and uses that UID to load and protect user-specific data.

That means:

1. Firebase handles sign-in and sign-up.
2. Angular keeps the current Firebase user and ID token in memory.
3. Every backend request includes the Firebase token in the `Authorization` header.
4. Spring verifies the token before serving protected routes.
5. Controllers and services use the verified Firebase UID to read and write the correct user's data.

## User flow

1. Authenticate with email/password or Google.
2. Create or load the backend profile.
3. Enter personal records.
4. Configure the workout cycle and days.
5. Review the dashboard, prescribed workout, and cycle history.
6. Use settings to refresh session state, inspect the backend user, or sign out.

## Frontend and backend

- Frontend: [ggains-frontend/README.md](./ggains-frontend/README.md)
- Backend: [ggains-backend/README.md](./ggains-backend/README.md)

## Main features

- Firebase sign-up, sign-in, and Google sign-in
- Profile onboarding and backend user creation
- Personal records management
- Workout cycle creation, update, history, and progression
- Prescribed workout lookup by date
- Authenticated app shell and settings area
