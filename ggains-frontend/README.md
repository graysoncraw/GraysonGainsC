# GraysonGains Frontend

Angular 22 frontend for GraysonGains. This app handles authentication, onboarding, the main training shell, and all API calls to the Spring backend.

## What this frontend does

- Signs users in with Firebase email/password or Google.
- Waits for Firebase auth state to restore after reload.
- Loads the backend user profile and keeps it in session state.
- Walks the user through onboarding:
  - profile
  - personal records
  - workout cycle setup
  - finish
- Shows the main app shell with dashboard, profile, PRs, cycles, and settings.
- Sends Firebase ID tokens to the backend on every protected API request.

## Local development

From `ggains-frontend/`:

```bash
npm install
npm start
```

Open `http://localhost:4200/`.

Other useful scripts:

```bash
npm run build
npm test
```

## Required configuration

Edit `src/app/auth/app-config.ts` before running the app locally.

- `BACKEND_API_BASE_URL` should point to the Spring backend, usually `http://localhost:8080`
- `FIREBASE_WEB_CONFIG` must match your Firebase web app configuration

If Firebase config is missing or still contains placeholders, auth initialization will fail and the app will not be able to authenticate.

## Main routes

- `/auth` - login and signup
- `/setup/profile` - create the backend user profile
- `/setup/personal-records` - enter PRs
- `/setup/cycles` - configure the training cycle
- `/setup/finish` - final onboarding handoff
- `/app` - main dashboard shell
- `/app/profile` - edit profile data
- `/app/personal-records` - edit personal records
- `/app/cycles` - view and progress the training cycle
- `/app/settings` - session tools and user lookup

## How the frontend is structured

### Auth flow

`FirebaseAuthService` is the core auth wrapper. It:

- initializes Firebase Auth
- listens for `onAuthStateChanged`
- keeps the current Firebase `User` in memory
- keeps the current ID token in memory
- exposes `ready`, `user`, `firebaseUid`, `idToken`, and `error`

The auth page uses that service directly, and the route guards wait for Firebase to finish restoring state before deciding where to send the user.

### Session flow

`SessionService` loads the backend user record for the current Firebase UID and stores it in memory. It also tracks session status so the shell and settings screen can show whether the backend profile is loading, ready, or errored.

### Onboarding flow

`OnboardingFlowService` decides which setup step the user should see next by checking:

1. Firebase auth state
2. backend user profile
3. personal record presence
4. workout cycle presence

This is used by the setup and app guards so users land on the correct screen automatically.

### API layer

The API services are thin wrappers around `HttpClient`:

- `UserApiService`
- `PersonalRecordApiService`
- `WorkoutCycleApiService`

### Auth token interceptor

`authTokenInterceptor` adds `Authorization: Bearer <Firebase ID token>` to backend requests. That is how the Spring backend knows which Firebase user is making the request.

## Notes

- Auth state lives in memory only now. Refreshing the page lets Firebase restore the session, but the app no longer depends on localStorage snapshots.
- The frontend is intentionally split into auth, session, onboarding, API, and page components so the flow stays easy to reason about.
