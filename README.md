# CodeForge - Coding Challenge & Interview Preparation Platform

CodeForge is a modern, high-performance coding challenge platform built for software engineers to sharpen their algorithmic intuition and prepare for technical interviews.

---

## Architecture Overview

The system follows a layered architecture ensuring separation of concerns, scalability, and secure code execution:

```
                  [ User / Web Browser ]
                 (Guest • Learner • Admin)
                            ↓
   1. FRONTEND LAYER — ReactJS / Web Application
   • Pages, Code Editor Interface, Dashboard, Axios REST
                            ↓ HTTPS / REST API (JSON)
   2. BACKEND LAYER — Java Spring Boot (Core Engine)
   • REST Controllers • Business Services • JWT Security • Integration Dispatcher
         ↓                       ↓                              ↓
3. DATABASE LAYER         Judge0 API                     Email Service
• Microsoft SQL Server    • Code Execution Engine        • External Notification
• Users, Exercises,       • Java & JS Sandbox Runner     • Password Reset & Alerts
  Submissions
```

---

## Directory Structure

```
mock-project-code/
├── frontend/                     # Complete Client-Side Application
│   ├── css/
│   │   └── styles.css            # Custom theme styles, dark IDE syntax colors, custom scrollbars
│   ├── js/
│   │   ├── app.js                # SPA Client router, table search & filter logic, auth state
│   │   ├── data.js               # Mock database, problems catalog, starter code, test cases
│   │   ├── ide.js                # Code editor, live timer, simulated test runner, submission modal
│   │   ├── editor.js             # Markdown editor toolbar, tag chips manager, difficulty selector
│   │   └── api.js                # REST API client with automatic offline fallback
│   ├── index.html                # Main single-page application entry point
│   ├── login.html                # Direct shortcut to the Sign in view
│   └── signup.html               # Direct shortcut to the Create account view
│
├── backend/                      # Server-Side Core Engine (Java Spring Boot)
│   └── README.md                 # Backend specification & architecture documentation
│
└── README.md                     # Root project documentation
```

---

## Completed Pages & Features

### 1. Dashboard / Overview (`#dashboard`)
* **Personalized Greeting**: Dynamic user header (`Keep the momentum, Hieu.`) with encouraging status.
* **Key Performance Metrics**:
  * *Problems Solved*: 48 (+8 this week)
  * *Current Streak*: 7 days (Personal best: 12)
  * *Acceptance Rate*: 76.2% (↑ 4.1%)
  * *Practice Time*: 12.5h (+2.4h this week)
* **Next Challenge Spotlight**: Features **Pair Sum** (#104, Arrays & Hashing, Step 13 of 20, ~15 min) with visual preview card (`target = 9`, `[ 2 ] [ 7 ]`, `O(n) is the goal`) and direct **`→ Continue solving`** CTA.
* **Weekly Goal Tracker**: Radial progress bar showing 8 / 10 problems completed, along with Monday-to-Sunday checkmark indicators.
* **Learning Paths**: Structured courses (*Arrays & hashing* at 60%, *Two pointers* at 33%, *Trees & graphs* at 11%).
* **Recent Activity**: Live log of accepted submissions with runtime in ms and memory consumption.

### 2. Problem Library (`#library`)
* **Curated Problem Catalog**: 240 algorithmic challenges covering all major interview topics.
* **Progress Banner**: 48 solved (20% of library) with difficulty distribution:
  * Easy: 32 / 80
  * Medium: 14 / 120
  * Hard: 2 / 40
* **Action Buttons**: `+ Create problem` and `🎲 Pick a random problem`.
* **Search & Filters**:
  * Keyboard shortcut `⌘ K` focus on search input.
  * Tab filtering: `All problems 240`, `Saved 18`, `My problems 3`.
  * Dropdown filters: Difficulty (Easy, Medium, Hard), Topics, and Status.
* **Interactive Problems Table**: Displays Status, ID, Title, Difficulty badge, Topic pills, Acceptance rate, and interactive Bookmark toggle.

### 3. Problem Solving IDE (`#ide`)
* **Subheader Bar**: Back navigation `← Problem library`, problem metadata (`104. Pair Sum`, `In progress`), pagination `< 13 / 20 >`, **live counting timer** `⏱ 00:12:48`, and **Focus Mode** toggle for distraction-free full-screen coding.
* **Left Pane (Problem Specification)**:
  * Tabs: *Description*, *Editorial*, *Solutions*, *Submissions*.
  * Rich problem statement, Example 1, Example 2 with formatted inputs and expected outputs, and mathematical constraints.
* **Right Pane (Code Editor & Test Runner)**:
  * Dark forest-green editor matching JetBrains Mono theme.
  * Line numbers, active line indicator (line 9), cursor position counter (`Ln 9, Col 43`).
  * Language selector (`Python 3`, `JavaScript`, `C++`).
  * Action controls:
    * **`▷ Run code`**: Runs code against sample test cases in ~32ms with visual feedback.
    * **`↑ Submit solution`**: Judges solution against full test suite (35 test cases) and triggers celebratory **Accepted** modal with runtime percentile (beats 89.4%) and memory percentile (beats 74.8%).
* **Bottom Test Console**:
  * Collapsible panel with *Test cases* and *Test results* tabs.
  * Displays pass/fail status, runtime, and comparison of Input, Your output, and Expected output.

### 4. Challenge Creator (`#create-problem`)
* **Live Status Indicator**: `☁ All changes saved · Last saved just now`, Live version 1.2, Unpublished changes badge.
* **Problem Details Card**:
  * Problem Title input.
  * Markdown-supported problem statement editor with formatting toolbar (Bold, Italic, Link, List, Code `< / >`) and real-time **Write / Preview** toggle.
* **Configuration Card**:
  * Default language selector.
  * Difficulty buttons (`Easy`, `Medium`, `Hard`).
  * Interactive Topic tags with chip management (add via Enter/comma, remove with `×`).
  * Time limit (`1,000 ms`) and Memory limit (`256 MB`) configurations.
* **Actions**: `👁 Preview`, `Save draft`, and `↗ Publish changes`.

### 5. Sign in / Authentication (`#login`)
* Sleek branded authentication card.
* Social login options: **Continue with GitHub** and **Continue with Google**.
* Form fields: Email / Username, Password, Remember me for 30 days, Forgot password link.
* Interactive submit button with loading spinner and session persistence.
* Quick switch to the Sign up page.

### 6. Sign up / Registration (`#signup`)
* Social signup integration with GitHub and Google.
* Form fields: Full Name, Email Address, Password with **Dynamic Password Strength Meter** (Good/Strong).
* Terms of Service and Privacy Policy agreement checkbox.
* Instant transition into the authenticated dashboard upon registration.

---

## Getting Started

### Running the Frontend
The frontend is completely self-contained and requires **no build step or package installations**:

1. Navigate to the `frontend/` directory.
2. Open [`frontend/index.html`](frontend/index.html) in any modern web browser (Google Chrome, Microsoft Edge, Firefox, Safari).
3. Alternatively, serve using any static file server:
   ```bash
   # Optional: using Python
   python -m http.server 3000 --directory frontend

   # Optional: using Node.js serve
   npx serve frontend
   ```

---

## Technologies Used

* **HTML5 & Modern DOM APIs**: Clean, semantic markup.
* **Tailwind CSS**: Utility-first responsive styling and typography.
* **JavaScript (ES6+)**: Modular Vanilla JS architecture (SPA routing, reactive UI state).
* **JetBrains Mono & Inter Fonts**: Monospace code clarity and modern UI typography.
* **Lucide Icons**: SVG interface iconography.
