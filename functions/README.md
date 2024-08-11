## Functions Overview

This project includes several Firebase Functions designed to interact with **Gemini API**, Firebase Firestore, Google Translate and Google Cloud Secret Manager. Below is a description of each function and its role within the application.

### 1. `generateChallenges`

**Trigger**:  
HTTP request (scheduled weekly)

**Purpose**:  
Generates weekly challenges based on SDGs using **Gemini API** and stores them in Firestore.

**Key Operations**:
- Initializes Google services and Firestore.
- Retrieves SDGs from Firestore.
- Generates challenge content for each SDG using *Gemini 1.5 Flash model*.
- Translates the challenge content into multiple languages.
- Stores the challenges in Firestore.

---

### 2. `generateSummary`

**Trigger**:  
HTTP request (scheduled daily)

**Purpose**:  
Generates daily motivational content related to one of the SDGs using **Gemini API** and stores it in Firestore.

**Key Operations**:
- Initializes Google services and Firestore.
- Fetches the last stored summary to determine the starting date.
- Generates a motivational content piece using *Gemini 1.5 Flash model*.
- Translates the content into multiple languages.
- Stores the translated summaries in Firestore.

---

### 3. `evaluateSubmission`

**Trigger**:  
Firestore document creation in the `challenge_entries` collection

**Purpose**:  
Evaluates user submissions to challenges by using **Gemini API** for analyzing the content and images against challenge criteria.

**Key Operations**:
- Initializes Google services and Firestore.
- Fetches challenge details from Firestore.
- Generates an evaluation using *Gemini 1.5 Flash model*, considering the submission's relevance to the challenge.
- Updates the submission document with scores, total score, and an evaluation summary.

---

### 4. `translateDocuments`

**Trigger**:  
HTTP request

**Purpose**:  
Translates the content of documents within the `sdg` collection in Firestore into multiple languages and updates the documents with the translations.

**Key Operations**:
- Accesses the `sdg` collection in Firestore.
- Retrieves and translates the `title` and `body` fields into target languages.
- Updates the documents with the translated content.

---

### 5. `translateAppDetailsDocuments`

**Trigger**:  
HTTP request

**Purpose**:  
Similar to `translateDocuments`, this function translates the content of documents within the `appDetails` collection in Firestore and updates them with the translations.

**Key Operations**:
- Accesses the `appDetails` collection in Firestore.
- Retrieves and translates the `title` and `body` fields into target languages.
- Updates the documents with the translated content.

---

### 6. `fileToGenerativePart(path, mimeType)`

**Purpose**:  
Converts a local file into a format compatible with Google Generative AI.

**Key Operations**:
- Reads the file from the local path.
- Encodes the file content in base64 format.
- Returns the encoded file as a `GoogleGenerativeAI.Part` object.

---

### 7. `downloadImage(url)`

**Purpose**:  
Downloads an image from a given URL and saves it to the local filesystem.

**Key Operations**:
- Extracts the file name from the URL.
- Saves the image to the local filesystem.
- Handles errors during the download process.

---

### 18. `initialize()`

**Purpose**:  
This function initializes and authenticates various Google services, including Google Cloud Secret Manager, Firestore, and Storage. It also retrieves the API key for Google Generative AI from the Secret Manager to enable **Gemini API** usage.

**Key Operations**:
- Accesses the secret API key from Google Cloud Secret Manager.
- Initializes the Google Generative AI client for Gemini Flash 1.5 model.
- Returns initialized services (Generative AI client, Firestore, Storage).

---

### 9. `getSDGs()`

**Purpose**:  
Fetches a list of Sustainable Development Goals (SDGs) from Firestore, ordered by their index field.

**Key Operations**:
- References the `sdg` collection in Firestore.
- Retrieves and orders documents by the `index` field.
- Returns an array of SDG goals.

---

## Notes:
- Make sure to replace `YOUR_GOOGLE_CLOUD_SECRET_MANAGER_PATH_FOR_GEMINI_API_KEY` with your Google Cloud Secret Manager path for your Gemini API key.
- Currently, all Firebase Functions are Gen 1.
- Scheduling is done in Google Cloud Console.
