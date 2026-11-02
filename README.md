# FIN-AI 2.0 — Smart Finance Management System

FIN-AI helps users track income and spending, set budgets and savings goals, and get AI-based advice.
It has two clients that share one backend and one database:

- **Website** (this folder) — Next.js 14 app with the REST API, deployed on Render:
  https://finai-smartai.onrender.com
- **Android app** ([`android-app/`](android-app)) — native Kotlin + Jetpack Compose app that reads bank
  transaction SMS on the phone and syncs with the same API.

```
Android app (Kotlin) ─┐
                      ├─► Next.js API on Render ─► MongoDB Atlas
Website (Next.js) ────┘           │
                                  ├─► Firebase Authentication (token check)
                                  └─► Groq API (insights, chat, bill reading)
```

## Features

**Website**
- Dashboard, transaction ledger, analytics charts, savings goals and finance news
- AI financial advisor and chat (Groq API)
- Bill and receipt scanning (image or PDF) to create a debit entry
- Monthly category budgets, bill splitting, transaction notes
- Admin console

**Android app**
- Automatic capture of bank SMS with a 5-step check against fake or phishing messages
- One linked mobile number per account, server-side duplicate removal
- Bill scan, analytics, budgets with alerts, subscription detector, savings goals
- AI advisor with a health score, AI chat, finance news
- No-spend streaks, badges, Monthly Wrapped and a home-screen widget

## Repository layout

| Path | Contents |
|---|---|
| `app/` | Next.js pages (`app/dashboard`, `app/auth`) and API routes (`app/api/**/route.ts`) |
| `components/` | React UI components |
| `lib/` | MongoDB connection, Firebase Admin token check, Groq client, parsers |
| `models/` | Mongoose schemas: User, Transaction, Goal, Budget, Split |
| `middleware.ts` | Route protection for the dashboard |
| `android-app/` | Android Studio project (Kotlin, Jetpack Compose) |

## Running the website

1. `npm install`
2. Create `.env.local` with your own values:
   ```env
   MONGODB_URI=
   GROQ_API_KEY=
   GNEWS_API_KEY=
   ENCRYPTION_KEY=

   NEXT_PUBLIC_FIREBASE_API_KEY=
   NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=
   NEXT_PUBLIC_FIREBASE_PROJECT_ID=
   NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=
   NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=
   NEXT_PUBLIC_FIREBASE_APP_ID=
   FIREBASE_ADMIN_PROJECT_ID=
   FIREBASE_ADMIN_CLIENT_EMAIL=
   FIREBASE_ADMIN_PRIVATE_KEY=

   ADMIN_EMAIL=
   ADMIN_PASSWORD=
   ADMIN_SECRET=
   NEXT_PUBLIC_ADMIN_SECRET=

   # optional: Gmail receipt sync
   GOOGLE_CLIENT_ID=
   GOOGLE_CLIENT_SECRET=
   GOOGLE_REDIRECT_URI=
   ```
3. `npm run dev` and open http://localhost:3000

## Running the Android app

1. Open the `android-app/` folder in Android Studio.
2. Debug builds call `http://localhost:3000` (the website running on your PC) through
   `adb reverse tcp:3000 tcp:3000`. To use the deployed site instead, set
   `finai.useLiveApi=true` in `android-app/gradle.properties`. Release builds always use Render.
3. Run on an emulator or a phone (Android 7.0 / API 24 or newer).

Unit tests: `./gradlew test` inside `android-app/`.

## Team

Aditya Hada (23BT04036), Het Vaghela (23BT04038), Tirth Joshi (23BT04049), Palak Patel (23BT04108)
— Minor Project II, GSFC University, guided by Dr. Poonam Shah.
