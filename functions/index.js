// Importing necessary modules
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const {getStorage} = require("firebase-admin/storage");
const { GoogleGenerativeAI } = require("@google/generative-ai");
const { Storage } = require('@google-cloud/storage');
const { SecretManagerServiceClient } = require('@google-cloud/secret-manager');
const { Translate } = require('@google-cloud/translate').v2;


const client = new SecretManagerServiceClient();
const { readFileSync } = require("fs");
const axios = require('axios');
const fs = require('fs');
const path = require('path');

// Initialize Firebase Admin SDK
admin.initializeApp();
const db = admin.firestore();
const storage = new Storage();

// Function to initialize and authenticate Google services
async function initialize() {
  try {
    // Access secret version from Google Cloud Secret Manager
    const [geminiVersion] = await client.accessSecretVersion({ name: 'YOUR_GOOGLE_CLOUD_SECRET_MANAGER_PATH_FOR_GEMINI_API_KEY' });
    const geminiApiKey = geminiVersion.payload.data.toString();
    const genAI = new GoogleGenerativeAI(geminiApiKey);

    // Return initialized services
    return {
      genAI,
      db,
      storage
    };
  } catch (error) {
    console.error('Error initializing functions:', error);
    throw new Error('Failed to initialize');
  }
}

// Function to get SDGs from Firestore
async function getSDGs() {
  try {
    // Reference to the 'sdg' collection in Firestore
    const sdgsRef = db.collection('sdg');
    const snapshot = await sdgsRef.orderBy('index').get();

    // If no SDGs found, return an empty array
    if (snapshot.empty) {
      console.log('No SDGs found.');
      return [];
    }

    // Map the documents to an array of SDGs
    const sdgsArray = snapshot.docs.map(doc => {
      const data = doc.data();
      return `SDG ${data.index}: ${data.goal}`;
    });

    return sdgsArray;
  } catch (error) {
    console.error('Error fetching SDGs: ', error);
    return [];
  }
}

// Cloud function to generate challenges based on SDGs
exports.generateChallenges = functions.https.onRequest(async (req, res) => {
  try {
    // Initialize Google services and Firestore
    const { genAI, db } = await initialize();
    const translate = new Translate();

    // Configuration for content generation
    const generationConfigForChallenges = {
      temperature: 1,
      topP: 0.95,
      topK: 64,
      maxOutputTokens: 8192,
      responseMimeType: "application/json",
    };

    // Initialize generative AI model
    const modelForChallenges = genAI.getGenerativeModel({
      model: "gemini-1.5-flash",
      systemInstruction: "Easy to understand. Be positive, no hatred.",
      generationConfig: generationConfigForChallenges,
    });

    // Fetch SDGs from Firestore
    const sdgs = await getSDGs();

    const languages = ["en", "hi", "ru", "fr", "zh", "es", "ar"]; // Add more languages as needed

    for (const sdg of sdgs) {
      // Get the last 5 challenges for the specific SDG
      const previousChallengesSnapshot = await db.collection('challenges')
        .where('sdg', '==', sdg)
        .orderBy('startDate', 'desc')
        .limit(5)
        .get();

      const previousChallenges = previousChallengesSnapshot.docs.map(doc => doc.data());

      // Prompt for generating a new challenge
      const prompt = `Generate a weekly challenge related to SDG "${sdg}" that users can complete at home.
      Here are the previous challenges: ${JSON.stringify(previousChallenges)}. Try not to repeat the previous challenges.
      The challenge should include title, description, evaluation criteria and points system.
      The JSON response must be formatted as below:
      {
        "title": "...",
        "description": "...",
        "evaluationCriteria": [
          {
            "criteria": "...",
            "maxPoints": "5"
          },
          { "criteria": "...",
            "maxPoints": "5"
          },
          { "criteria": "...",
            "maxPoints": "5"
          }
        ]
      }
      `;

      // Generate the challenge content using AI
      const result = await modelForChallenges.generateContent([{ text: prompt }]);
      const response = result.response;
      const ans = await response.text();

      // Parse the generated JSON response
      const challenge = JSON.parse(ans);

      // Calculate start and end dates for the challenge
      const today = new Date();
      const startDate = today.toISOString().split('T')[0];
      const nextWeek = new Date();
      nextWeek.setDate(today.getDate() + 7);
      const endDate = nextWeek.toISOString().split('T')[0];

      // Prepare translations object
      const translations = {
        title: { en: challenge.title },
        description: { en: challenge.description },
        evaluationCriteria: challenge.evaluationCriteria.map(criteria => ({
          criteria: { en: criteria.criteria },
          maxPoints: criteria.maxPoints,
        })),
      };

      // Translate the challenge content into other languages
      for (const language of languages) {
        if (language === 'en') continue;

        const [translatedTitle] = await translate.translate(challenge.title, language);
        const [translatedDescription] = await translate.translate(challenge.description, language);

        const translatedEvaluationCriteria = await Promise.all(challenge.evaluationCriteria.map(async (criteria, index) => {
          const [translatedCriteria] = await translate.translate(criteria.criteria, language);
          translations.evaluationCriteria[index].criteria[language] = translatedCriteria;
        }));

        translations.title[language] = translatedTitle;
        translations.description[language] = translatedDescription;
      }

      // Store the generated challenge in Firestore
      await db.collection('challenges').add({
        sdg,
        title: translations.title,
        description: translations.description,
        evaluationCriteria: translations.evaluationCriteria,
        startDate: startDate,
        endDate: endDate,
        active: true,
      });
    }

    res.status(200).send('Challenges generated and stored successfully');
  } catch (error) {
    console.error('Error generating challenges:', error);
    res.status(500).send('Error generating challenges');
  }
});

// Cloud function to generate a motivational summary related to SDGs
exports.generateSummary = functions.https.onRequest(async (req, res) => {
  try {
    // Initialize Google services and Firestore
    const { genAI, db, storage } = await initialize();
    const translate = new Translate();

    // Configuration for content generation
    const generationConfig = {
      temperature: 1,
      topP: 0.95,
      topK: 64,
      maxOutputTokens: 8192,
      responseMimeType: "text/plain",
    };

    // Initialize generative AI model
    const model = genAI.getGenerativeModel({
      model: "gemini-1.5-flash",
      systemInstruction: "Easy to understand. Be positive, no hatred. Empathize and motivate.",
      generationConfig: generationConfig,
    });

    // Fetch the last stored summary to get the date
    const lastSummarySnapshot = await db.collection('summaries')
      .orderBy('date', 'desc')
      .limit(1)
      .get();

    let lastDate = null;
    if (!lastSummarySnapshot.empty) {
      const lastSummaryDoc = lastSummarySnapshot.docs[0];
      lastDate = lastSummaryDoc.data().date;
    }

    // Set the current date to one day after the last date
    const currentDate = lastDate ? new Date(lastDate) : new Date();
    currentDate.setDate(currentDate.getDate() + 1);
    const formattedDate = currentDate.toISOString().split('T')[0];

    // Generate the summary content using AI
    const result = await model.generateContent([
      {
        text: `Please generate a motivational and positive content piece about one of the 17 SDGs. Avoid any negative or hateful language. The tone should be warm, encouraging, and empowering. Aim for a length of about 1,000 to 1,500 words.`
      }
    ]);

    const response = result.response;
    const summary = await response.text();

    if (!summary) {
      throw new Error('Summary generation failed. The response is empty or malformed.');
    }

    // Use the first line of the summary as the title
    const title = summary.split('\n')[0].trim();

    // Initialize the translation maps
    const titleMap = { en: title };
    const summaryMap = { en: summary };

    // List of languages for translation
    const languages = ['hi','fr','es','ar','ru','zh']; // Add more language codes as needed

    // Translate title and summary into other languages
    for (const lang of languages) {
      const [translatedTitle] = await translate.translate(title, lang);
      const [translatedSummary] = await translate.translate(summary, lang);
      titleMap[lang] = translatedTitle;
      summaryMap[lang] = translatedSummary;
    }

    // Store the title and summary maps in Firestore under the document ID of the formattedDate
    await db.collection('summaries').doc(formattedDate).set({
      date: formattedDate,
      title: titleMap,
      summary: summaryMap,
    }, { merge: true });

    res.status(200).send('Summary generated and stored successfully');
  } catch (error) {
    console.error('Error generating summary:', error);
    res.status(500).send('Error generating summary');
  }
});


// Cloud Function to evaluate user submissions when a new document is created in 'challenge_entries'
exports.evaluateSubmission = functions.firestore.document('challenge_entries/{entryId}')
  .onCreate(async (snap, context) => {
    try {
      // Initialize the Google services (Generative AI, Firestore, and Storage)
      const { genAI, db, storage } = await initialize();

      // Configuration for content generation with the AI model
      const generationConfigForEvaluation = {
        temperature: 1, 
        topP: 0.95,     
        topK: 64,       
        maxOutputTokens: 8192, 
        responseMimeType: "application/json", 
      };

      // Initialize the Generative AI model for evaluation
      const modelForEvaluation = genAI.getGenerativeModel({
        model: "gemini-1.5-flash", // Model name/version
        systemInstruction: "Easy to understand.", // Instruction to keep output simple and clear
        generationConfig: generationConfigForEvaluation,
      });

      // Extract data from the newly created entry document
      const entry = snap.data();
      const challengeId = entry.challenge_id;
      const entryDescription = entry.entry_description;
      const photoUrls = entry.photo_urls;
      const localLanguage = entry.localLanguage; // User's local language for summary
      const localLanguageCode = entry.localLanguageCode; // Language code for translation

      // Fetch the challenge details from Firestore using the challenge ID
      const challengeDoc = await db.collection('challenges').doc(challengeId).get();
      if (!challengeDoc.exists) {
        throw new Error('Challenge not found'); // Throw an error if challenge does not exist
      }
      const challenge = challengeDoc.data();

      // Extract English content for the challenge (title, description, and evaluation criteria)
      const englishTitle = challenge.title['en'];
      const englishDescription = challenge.description['en'];
      const englishEvaluationCriteria = challenge.evaluationCriteria;

      // Construct a prompt to guide the AI in evaluating the submission
      let prompt = `Evaluate the following user submission based on the challenge details:
      Challenge Title: ${englishTitle}
      Description: ${englishDescription}`;

      prompt += `\nUser Submission Description: ${entryDescription}
      User Submission Images: (Attached images)

      Instructions:
      1. Verify Relevance: Carefully check if the attached images are relevant to the challenge description and user entry. The images should clearly relate to challenge description and user entry.
      2. Evaluate Each Criterion: \n`;

      // Append each evaluation criterion to the prompt
      englishEvaluationCriteria.forEach((criteria, index) => {
        prompt += `(${index + 1}) ${criteria.criteria.en}: max ${criteria.maxPoints} points\n`;
      });

      // Append final instructions to provide scores and generate a summary in the user's local language
      prompt += `\n3. Provide Scores: Assign scores for each above-mentioned criterion based on the relevance and quality of the submission. Be strict in assigning scores.
                 4. Total Score and Summary: Calculate the total score by summing up the individual scores for above-mentioned criteria. Write a short evaluation summary highlighting the strengths and weaknesses of the submission, especially in relation to the images provided. Be polite and encouraging in the summary.
                 The JSON output must be as follows:
                 {
                 "scores": [
                 {
                 "criteria": "...",
                 "score": int
                 },
                 {
                 "criteria": "...",
                 "score": int
                 },
                 ...
                 ],
                 "totalScore": int,
                 "summary": "..."
                 }
                 Do not change the wording of criteria at all.
                 Give summary in ${localLanguage} only.
      `;

      // console.log('Generated Prompt:', prompt);

      // Process each photo URL by downloading the image and converting it to a format suitable for the AI model
      const imageParts = [];
      for (let i = 0; i < photoUrls.length; i++) {
        const imagePath = await downloadImage(photoUrls[i]);
        imageParts.push(fileToGenerativePart(imagePath, "image/jpeg")); // Convert image to Generative Part format
      }

      // Generate the evaluation content by passing the prompt and image parts to the AI model
      const result = await modelForEvaluation.generateContent([prompt, ...imageParts]);
      const evaluationResponse = await result.response.text();
      const evaluation = JSON.parse(evaluationResponse); // Parse the AI response as JSON

      // Log the generated evaluation for debugging
      // console.log('Generated evaluation:', evaluation);

      try {
        // Update the original entry document with the evaluation results (scores, total score, summary, and timestamp)
        await snap.ref.update({
          scores: evaluation.scores,
          totalScore: evaluation.totalScore,
          summary: evaluation.summary,
          evaluatedAt: admin.firestore.FieldValue.serverTimestamp() // Timestamp of evaluation
        });

        console.log('Submission evaluated successfully'); // Log success message
      } catch (e) {
        console.error(e); // Log any error that occurs during the update
      }
    } catch (error) {
      // Handle any errors that occur during the evaluation process
      console.error('Error evaluating submission:', error);
    }
  });



// Converts local file information to a GoogleGenerativeAI.Part object.
function fileToGenerativePart(path, mimeType) {
  return {
    inlineData: {
      data: Buffer.from(readFileSync(path)).toString("base64"),
      mimeType
    },
  };
}

// Function to download an image from a given URL and save it to the local filesystem
async function downloadImage(url) {
  try {
    // Extract the file name from the URL (removing any query parameters)
    const fileName = url.split('/').pop().split('?')[0];
    // Determine the destination file path where the image will be saved
    const destFileName = path.join('/tmp', fileName);

    console.log(`Downloading from URL ${url} to ${destFileName}`);

    // Check if the directory exists; if not, create it
    if (!fs.existsSync(path.dirname(destFileName))) {
      fs.mkdirSync(path.dirname(destFileName)); // Create the directory
    }

    // Make an HTTP GET request to download the image
    const response = await axios({
      url, // The image URL
      method: 'GET', // HTTP method
      responseType: 'stream' // Stream the response data directly to a file
    });

    // Pipe the response data (image stream) to a file on the local filesystem
    response.data.pipe(fs.createWriteStream(destFileName));

    // Return a promise that resolves when the download is complete, or rejects if there's an error
    return new Promise((resolve, reject) => {
      // When the stream ends, resolve the promise with the destination file path
      response.data.on('end', () => {
        console.log('Download successful:', destFileName);
        resolve(destFileName);
      });
      // If there's an error during the download, reject the promise with the error
      response.data.on('error', err => {
        console.error('Error downloading image:', err);
        reject(err);
      });
    });

  } catch (error) {
    // Log and re-throw any errors that occur during the download process
    console.error('Error downloading image:', error);
    throw error;
  }
}


// Cloud Function to translate documents in Firestore and update them with translations
exports.translateDocuments = functions.https.onRequest(async (req, res) => {
  // Access the 'sdg' collection in Firestore
  const sdgCollection = db.collection('sdg');

  // Retrieve all documents within the 'sdg' collection
  const sdgDocs = await sdgCollection.get();
  const translate = new Translate(); // Initialize the Google Translate API client

  // Loop through each document in the 'sdg' collection
  for (const doc of sdgDocs.docs) {
    const sdgId = doc.id; // Get the document ID

    // Access the 'details' subcollection within the current document
    const detailsCollection = doc.ref.collection('details');
    const detailsDocs = await detailsCollection.get();

    // Loop through each document in the 'details' subcollection
    for (const detailDoc of detailsDocs.docs) {
      const detailId = detailDoc.id; // Get the detail document ID
      const data = detailDoc.data(); // Get the data from the detail document

      // Extract the English version of the title and body
      const title = data.title.en;
      const body = data.body.en;

      // Define the target languages for translation
      const targetLanguages = ['es', 'fr', 'hi', 'ru', 'zh', 'ar']; // Spanish, French, Hindi, Russian, Chinese, Arabic

      // Loop through each target language and perform translations
      for (const lang of targetLanguages) {
        // Translate the title and body to the current target language
        const [translatedTitle] = await translate.translate(title, lang);
        const [translatedBody] = await translate.translate(body, lang);

        // If translations are successful, update the document with the translations
        if (translatedTitle && translatedBody) {
          await detailDoc.ref.update({
            [`title.${lang}`]: translatedTitle, // Update the title in the target language
            [`body.${lang}`]: translatedBody   // Update the body in the target language
          });
        }
      }
    }
  }

  console.log('Translation process completed.'); // Log completion of the translation process
});


// Function to translate app details!
exports.translateAppDetailsDocuments = functions.https.onRequest(async (req, res) => {
  const sdgCollection = db.collection('appDetails');

  const sdgDocs = await sdgCollection.get();
  const translate = new Translate();

for (const detailDoc of sdgDocs.docs) {
     const detailId = detailDoc.id;
     const data = detailDoc.data();

     // Get title and body in English
     const title = data.title.en;
     const body = data.body.en;

     // Define target languages
     const targetLanguages = ['es', 'fr', 'hi', 'ru', 'zh', 'ar'];

     for (const lang of targetLanguages) {
       // Translate title and body
       const [translatedTitle] = await translate.translate(title, lang);
       const [translatedBody] = await translate.translate(body, lang);


       if (translatedTitle && translatedBody) {
         // Update the document with the translations
         await detailDoc.ref.update({
           [`title.${lang}`]: translatedTitle,
           [`body.${lang}`]: translatedBody
         });
       }
     }
   }
 console.log('Translation process completed.');
});