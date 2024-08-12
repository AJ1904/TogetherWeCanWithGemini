# Together We Can With Gemini


## Features of the Gemini API Integration (short version)

- **Personalized Content Creation**: The **ActivityViewModel** leverages the **Gemini 1.5-flash** model to generate tailored social media content based on user activity and images. This ensures that content is not only relevant but also localized, with personalized hashtags and emojis to boost engagement.

- **Dynamic Challenge Generation**: The **ChallengesViewModel** utilizes the Gemini API to produce unique weekly challenges related to the 17 Sustainable Development Goals (SDGs). Each challenge is dynamically generated, avoiding repetition, and is translated into the user’s local language for broader accessibility.

- **Localized Quiz and Task Management**: The **SDGViewModel** employs the Gemini API to generate quiz questions and task suggestions for SDGs. Quizzes come with multiple-choice options and explanations in the user's language, while tasks are categorized by difficulty and tailored to support user efforts in achieving SDG goals.

- **Step-by-Step Goal Guidance**: The Gemini API provides actionable steps for achieving SDGs, including location-based activities. By integrating with Google Maps Places API, the application offers relevant place recommendations based on the user's location.

- **Multilingual Content Delivery**: The **generateChallenges** and **generateSummary** Firebase functions use the Gemini API to create and translate content into multiple languages. Weekly challenges and daily motivational summaries are both generated and translated, ensuring that content is accessible to users worldwide.

- **Comprehensive Submission Evaluation**: The **evaluateSubmission** function assesses user entries for challenges using the Gemini API. It evaluates textual and visual submissions, providing detailed feedback and scores in the user’s local language.

- **Automatic Document Translation**: The **translateDocuments** and **translateAppDetailsDocuments** functions leverage Google Translate to ensure that SDG-related documents and app details are available in multiple languages, enhancing global accessibility and user engagement.


## Gemini API in Action (long version)

- **`ActivityViewModel`**: This ViewModel in the Android application is designed to manage UI state while leveraging the Google **Gemini** API for advanced content generation. It uses the **Gemini 1.5-flash** model, which is configured to produce personalized social media content based on user activity details and images. The ViewModel is capable of generating content in the user's **local language**, ensuring that the response from the **Gemini API** is not only contextually relevant but also linguistically appropriate for the user's audience. This feature allows for the inclusion of localized hashtags and emojis, further enhancing the content's engagement potential.

- **`ChallengesViewModel`**: This ViewModel is responsible for managing and fetching weekly challenges from Firestore, which are dynamically generated using a Firebase function powered by the **Gemini API**. Specifically, the **Gemini 1.5 Flash** model generates new challenges based on the 17 Sustainable Development Goals (SDGs), ensuring that each week's challenge is unique and non-repetitive. These challenges include a title, description, and evaluation criteria, all of which are automatically translated into the user's local language using Google Translate. The ViewModel retrieves these localized challenges from Firestore, enabling users to engage with the content in their preferred language. Additionally, the **Gemini API** response is tailored to the user's locale, making the experience seamless and personalized.

- **`SDGViewModel`**: This ViewModel is designed to facilitate interaction with Sustainable Development Goals (SDGs) by leveraging the Gemini API. This ViewModel provides functionalities for generating tasks and quiz questions, suggesting actionable steps for SDG-related goals, and fetching detailed information about SDGs from Firestore. 

    - **Quiz Generation**: The SDGViewModel uses the Gemini 1.5 Flash model to generate quiz questions related to specific SDGs. Each question comes with multiple-choice options, a correct answer, and explanations for each option. The Gemini API responds in the local language of the user for better comprehension. Function is `generateQuizQuestion`.

    - **Task Suggestions**: For each SDG, the Gemini API provides tasks categorized into easy, medium, and difficult levels. The Gemini 1.5 Flash model generates these tasks to help users contribute effectively to the goals. Function is `getActionsForSDG`.

    - **Step-by-Step Guidance**: Users can get actionable steps to achieve specific goals using Gemini API. The Gemini 1.5 Flash model also confirms if the steps involve location-based activities, thereby utilizes the Google Maps Places API to find relevant places based on the user's location. Functions are `suggestStepsForGoal` and `isThisStepRelatedToMaps`.

    - **Local Language Support**: All responses from the Gemini API are in the user's locale language, ensuring accessibility and clarity.


- **`generateChallenges`**: This is a Firebase function which uses the **Gemini API** to generate weekly challenges based on 17 Sustainable Development Goals (SDGs). The Gemini 1.5 Flash model creates new challenges (title, description, evaluation criteria) while ensuring it doesn't repeat past challenges. All challenges are translated into multiple languages using Google Translate and stored in Firestore.

- **`generateSummary`**: This is a Firebase function which uses the **Gemini API** to generate daily motivational content related to SDGs. It also translates the content into various languages using Google Translate. The translated summaries are stored in Firebase Firestore, allowing for multilingual accessibility of SDG-related content.

- **`evaluateSubmission`**: When a user submits a challenge entry, this Firebase function evaluates the submission using the Google Gemini API, considering both textual descriptions and images. The Gemini 1.5 Flash model generates scores and a summary of the evaluation in the user's local language and updates the submission document in Firebase Firestore with these results.

- **`translateDocuments`** and **`translateAppDetailsDocuments`**: These Firebase functions automatically translates SDG-related documents and app-related details stored in Firestore into multiple languages using Google Translate. They ensure that all documents, their subcollections and fields have multilingual content, making the information accessible to a global audience.

## Notes:
- In the `local.properties` file, specify Android sdk path in `sdk.dir`, Gemini API key `apiKey` and Google Maps API Key `PLACES_API_KEY`.
- Make sure to replace `YOUR_GOOGLE_CLOUD_SECRET_MANAGER_PATH_FOR_GEMINI_API_KEY` in `functions/index.js` with your Google Cloud Secret Manager path for your Gemini API key.
- Currently, all Firebase Functions are Gen 1.
- Scheduling is done in Google Cloud Console.
- APK file present in https://drive.google.com/drive/folders/1jfb8h49ZLyTU7UzeypjuaRBGOAg_86Fg?usp=sharing


