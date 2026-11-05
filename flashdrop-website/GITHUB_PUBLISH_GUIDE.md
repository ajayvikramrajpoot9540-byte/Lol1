# 🚀 FlashDrop Website — Complete Code & Step-by-Step GitHub Publishing Guide

All of your official **FlashDrop Website** code is inside this **`/flashdrop-website`** folder and styled with the exact FlashDrop Android App color palette:
- **Primary Green:** `#16A34A` / `#0B6E4F`
- **Accent Orange:** `#FF8A24` / `#EA580C`
- **Official Logo:** `assets/flashdrop-logo.png` (proportionally scaled with `object-fit: contain`)

---

## 📂 Files Inside `/flashdrop-website`

| File / Folder | Purpose |
| :--- | :--- |
| `index.html` | Main website entry point (Customer Storefront, Shopkeeper Schedule Portal, Admin Portal, and GitHub Guide) |
| `styles.css` | Responsive CSS for phones, foldables, tablets, and desktops using FlashDrop Green & Orange colors |
| `app.js` | Interactive JavaScript engine (Camera Photo Search in Search Bar with **Least Rate / Lowest Price First** sorting, 30-Minute IST Forward Delivery calculator, 10 AM – 10 PM Auto Schedule, Cart & Checkout) |
| `assets/flashdrop-logo.png` | Official FlashDrop logo |
| `GITHUB_PUBLISH_GUIDE.md` | This step-by-step GitHub publishing guide |

---

## 🌐 Method 1: Publish on GitHub Pages in 2 Minutes (No Coding / No Terminal Needed)

This is the easiest way to get a live, public website link (`https://your-username.github.io/flashdrop-website/`).

### Step 1: Download Your Project ZIP from AI Studio
1. In the top-right corner of **Google AI Studio**, click the **Export / Download ZIP** option.
2. Unzip the downloaded folder on your computer or phone.
3. Open the **`flashdrop-website`** folder so you can see `index.html`, `styles.css`, `app.js`, and the `assets` folder.

### Step 2: Create a New Repository on GitHub
1. Open [https://github.com/new](https://github.com/new) in your browser and sign in to your GitHub account.
2. Under **Repository name**, type:
   ```text
   flashdrop-website
   ```
3. Make sure **Public** is selected.
4. Check **"Add a README file"** (optional) or leave it unchecked, then click the green **Create repository** button.

### Step 3: Upload the Website Files
1. On your newly created repository page, click the **"uploading an existing file"** link (or click **Add file** → **Upload files**).
2. Open the **`flashdrop-website`** folder on your computer, select **all files and folders inside it** (`index.html`, `styles.css`, `app.js`, `GITHUB_PUBLISH_GUIDE.md`, and the `assets` folder), and drag-and-drop them into the GitHub upload box.
   > **Important:** Make sure `index.html` is at the top level of the repository so GitHub Pages can open it automatically.
3. Scroll down and click the green **Commit changes** button.

### Step 4: Activate Free GitHub Pages Hosting
1. In your GitHub repository top menu, click **Settings** ⚙️.
2. In the left sidebar, click **Pages** (under the "Code and automation" section).
3. Under **Build and deployment** → **Branch**:
   - Select **`main`** (or `master`) from the branch dropdown.
   - Select **`/ (root)`** for the folder.
   - Click **Save**.
4. Wait about **60 seconds** and refresh the **Settings → Pages** screen.
5. You will see a banner saying:
   > **Your site is live at `https://<your-github-username>.github.io/flashdrop-website/`**

---

## 💻 Method 2: Publish Using Git Terminal (Command Line)

If you have **Git** installed on your computer, open your terminal inside the `flashdrop-website` folder and run these commands step by step:

```bash
# 1. Go into the website folder
cd flashdrop-website

# 2. Initialize a new Git repository
git init

# 3. Stage all website files (index.html, styles.css, app.js, assets/flashdrop-logo.png)
git add .

# 4. Commit the files
git commit -m "Initial commit: Official FlashDrop Website"

# 5. Set the main branch name
git branch -M main

# 6. Connect to your GitHub repository (replace YOUR_USERNAME with your GitHub username)
git remote add origin https://github.com/YOUR_USERNAME/flashdrop-website.git

# 7. Push your code to GitHub
git push -u origin main
```

After pushing, go to your GitHub repository's **Settings → Pages**, select the **`main`** branch and **`/ (root)`**, and click **Save** to make your website live!
