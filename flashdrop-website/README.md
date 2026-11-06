# 🛍️ FlashDrop Official Website & App Data Package

This folder contains the complete, standalone **FlashDrop Website** with all product and shop data synchronized directly from the Android App (`SeedData.kt`).

---

## 📂 Included Files & Data

| File / Folder | Description |
| :--- | :--- |
| **`index.html`** | Main responsive storefront with Home, Partner Shops, About Us, Terms & Conditions, and Contact pages |
| **`styles.css`** | Official FlashDrop branding in Green (`#16A34A` / `#0B6E4F`) and Orange (`#FF8A24` / `#EA580C`) |
| **`app.js`** | Interactive engine with **Working Mic Voice Search**, **Camera Photo Search**, and **Least Rate / Lowest Price First** sorting |
| **`data/app-data.json`** | Clean JSON export of all 11 verified partner shops and 26 products from the Android App |
| **`data/app-data.js`** | JavaScript file defining `window.FLASHDROP_APP_DATA` for zero-CORS offline & web hosting |
| **`data/products.csv`** | Spreadsheet format of all products (Prices, Categories, Subcategories, MRP, Units, Ratings) |
| **`data/shops.csv`** | Spreadsheet format of all partner shops (Addresses, Phones, Operating Hours, Delivery Fees) |
| **`assets/flashdrop-logo.png`** | Official high-resolution FlashDrop logo |
| **`.nojekyll`** | Ensures GitHub Pages serves all static data and assets directly |

---

## 🚀 How to Publish to GitHub Pages in 2 Minutes

1. Create a new repository on [GitHub.com](https://github.com/new) named `flashdrop-website` (Public).
2. Upload all files from this folder directly into the repository.
3. In your repository on GitHub, go to **Settings** ⚙️ → **Pages** → Branch: select **`main`** (`/root`) → click **Save**.
4. Your website will be live in 1 minute!
