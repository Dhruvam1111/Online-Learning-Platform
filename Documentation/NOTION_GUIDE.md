# 📘 Complete Guide: Notion for Sprint 1 Project Tracking

Welcome! If you have never worked with **Notion** before, this guide will walk you through everything step-by-step—from creating your account to setting up a professional Agile Kanban board for our **Online Learning Platform** sprint tracking.

---

## 🌟 What is Notion?

**Notion** is an all-in-one collaborative workspace that combines notes, docs, databases, and project boards. In software teams, Notion is widely used for:
- **Sprint Boards (Kanban):** Visualizing tasks moving from *To Do* → *In Progress* → *PR / Review* → *Done*.
- **Documentation & Meeting Notes:** Centralizing architecture decisions, sprint retrospectives, and API contracts.
- **Traceability:** Connecting User Stories (from our `Online_Learning_Platform_User_Stories.xlsx`) directly to pull requests and team members.

---

## 🚀 Step 1: Create a Free Notion Account

1. Go to [https://www.notion.so](https://www.notion.so).
2. Click **Get Notion Free** (or **Log In**).
3. Sign up using either:
   - Your college email (`202401415@dau.ac.in`) to get access to Notion's free educational plan features, OR
   - Your Google account.
4. When prompted "How do you plan to use Notion?", choose **"For my team"** or **"For personal use"** (both allow free sharing).
5. Name your workspace (e.g., `Online Learning Platform Team`).

---

## 📋 Step 2: Create the Sprint 1 Progress Board

### 1. Create a New Page
1. In the left sidebar, click **+ Add a page**.
2. Title the page: `Online Learning Platform — Sprint 1`.
3. Choose the **Board** template (or type `/board` and select **Board view**).
4. Click **+ New database**.

### 2. Set Up the Workflow Columns (Status)
Rename and organize the columns across the top of your board into these 5 standard Agile stages:
1. **📋 Backlog** — Tasks planned for future sprints or under evaluation.
2. **⏳ To Do (Sprint 1)** — Tasks committed for Sprint 1 that have not started yet.
3. **🚀 In Progress** — Tasks currently being coded.
4. **🔍 In Review / PR** — Code completed, pull request submitted, awaiting peer review.
5. **✅ Done** — Pull request merged, tests passing, verified in local/staging environment.

---

## ⚙️ Step 3: Configure Essential Card Properties

Click on any card on your board, or click the **Properties** button at the top of the board to add these metadata fields:

| Property Name | Property Type | Options / Purpose |
|---|---|---|
| **Status** | Status | *Backlog*, *To Do*, *In Progress*, *In Review / PR*, *Done* |
| **Assignee** | Person / Select | Dhruvam, Shubh, Pranamya, etc. |
| **Story ID** | Text | e.g., `US-S01`, `US-I01` (from Excel user stories) |
| **Component** | Select | `Backend (Auth)`, `Backend (Courses)`, `Frontend (React)`, `Database`, `DevOps/CI` |
| **Priority** | Select | `🔴 High`, `🟡 Medium`, `🟢 Low` |
| **PR Link** | URL | Direct link to GitHub PR (e.g., `https://github.com/Dhruvam1111/Online-Learning-Platform/pull/4`) |
| **Target Date** | Date | Deadline for the task |

---

## 📝 Step 4: Populate Sprint 1 Tasks

Here is the starter set of cards for our Sprint 1 to add to your board immediately:

### 1. Database Schema & CI Setup
- **Status:** `✅ Done`
- **Assignee:** Team
- **Component:** `Database`
- **Description:** PostgreSQL schema, seed data, indexes, and GitHub Actions CI workflow (Merged in PR #3).

### 2. Authorization Backend Service (JWT & Google OAuth2)
- **Status:** `🔍 In Review / PR`
- **Assignee:** Dhruvam Panchal, Shubh Patel
- **Component:** `Backend (Auth)`
- **PR Link:** `https://github.com/Dhruvam1111/Online-Learning-Platform/pull/4`
- **Description:** Spring Boot 3 authentication backend supporting JWT registration, login, and Google OAuth ID token verification.

### 3. Documentation & Notion Progress Tracking
- **Status:** `🔍 In Review / PR`
- **Assignee:** Dhruvam Panchal
- **Component:** `Documentation`
- **Description:** Updated README with architecture and setup instructions, plus complete Notion guide.

### 4. Course Management Backend
- **Status:** `⏳ To Do (Sprint 1)`
- **Assignee:** Assigned team member
- **Component:** `Backend (Courses)`
- **Description:** Course creation, draft/publish status management, and lecture ordering.

### 5. Frontend Authentication & Course Catalog UI
- **Status:** `⏳ To Do (Sprint 1)`
- **Assignee:** Assigned team member
- **Component:** `Frontend (React)`
- **Description:** React 18 + Vite frontend with login/registration forms, student course browsing, and course viewer.

### 6. YouTube Lecture Video Player Integration
- **Status:** `⏳ To Do (Sprint 1)`
- **Assignee:** Assigned team member
- **Component:** `Frontend (React)`
- **Description:** Embedded YouTube player rendering lectures according to `video_id`.

---

## 🔗 Step 5: Share the Board & Get the Link

To enable your teammates and mentors to view or edit the board:

1. Look at the top right of your Notion page and click the **Share** button.
2. You have two options:
   - **Option A (Team Collaboration - Recommended):** Type the email addresses of your team members (`202401478@dau.ac.in`, etc.) and set their permission to **Can edit**.
   - **Option B (Public Link for README & Mentors):** Click the **Publish** tab inside the Share menu and toggle **Publish to web**.
3. Click **Copy web link** (or **Copy link**).
4. The link will look like:
   ```
   https://www.notion.so/your-workspace/Sprint-1-Board-1234567890abcdef
   ```

---

## 🔄 Step 6: Updating README with Your Notion Link

Whenever you generate or change your Notion board link:
1. Open the project root `README.md`.
2. Locate the **📌 Project Progress & Sprint Tracking (Notion)** section:
   ```markdown
   - 🔗 **Sprint 1 Progress Board:** [Online Learning Platform — Sprint 1 Board (Notion)](https://www.notion.so/YOUR-LINK-HERE)
   ```
3. Replace the placeholder URL with your actual board link.
4. Commit and push the update to GitHub:
   ```bash
   git add README.md
   git commit -m "docs: Update Notion sprint tracking board link"
   git push origin dhruvam/documentation
   ```

---

## 💡 Daily Best Practices for the Team

- **Start of Day / Standup:** Move cards to **In Progress** when you begin writing code.
- **When Opening a PR:** Move the card to **In Review / PR** and paste the GitHub PR URL into the card's `PR Link` property.
- **After PR Merge:** Move the card to **Done**.
- **Inside Each Card:** You can write notes, paste curl requests, attach screenshots, or list questions for your teammates directly on the card!
