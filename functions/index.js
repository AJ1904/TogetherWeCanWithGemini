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

admin.initializeApp();
const db = admin.firestore();
const storage = new Storage();

// Initialize function
async function initialize() {
  try {
    const [geminiVersion] = await client.accessSecretVersion({ name: 'projects/175255734031/secrets/GEMINI_API_KEY/versions/latest' });
    const geminiApiKey = geminiVersion.payload.data.toString();
    const genAI = new GoogleGenerativeAI(geminiApiKey);

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

async function getSDGs() {
  try {
    const sdgsRef = db.collection('sdg'); // Reference to your 'sdgs' collection
    const snapshot = await sdgsRef.orderBy('index').get();

    if (snapshot.empty) {
      console.log('No SDGs found.');
      return [];
    }

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



exports.generateChallenges = functions.https.onRequest(async (req, res) => {
  try {
    const { genAI, db } = await initialize();
    const translate = new Translate();

    const generationConfigForChallenges = {
      temperature: 1,
      topP: 0.95,
      topK: 64,
      maxOutputTokens: 8192,
      responseMimeType: "application/json",
    };

    const modelForChallenges = genAI.getGenerativeModel({
      model: "gemini-1.5-flash",
      systemInstruction: "Easy to understand. Be positive, no hatred.",
      generationConfig: generationConfigForChallenges,
    });

    const sdgs = await getSDGs();

    const languages = ["en", "hi", "ru", "fr", "zh", "es", "ar"]; // Add more languages as needed

    for (const sdg of sdgs) {
      const previousChallengesSnapshot = await db.collection('challenges')
        .where('sdg', '==', sdg)
        .orderBy('startDate', 'desc')
        .limit(5)
        .get();

      const previousChallenges = previousChallengesSnapshot.docs.map(doc => doc.data());

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

      const result = await modelForChallenges.generateContent([{ text: prompt }]);
      const response = result.response;
      const ans = await response.text();

      const challenge = JSON.parse(ans);

      const today = new Date();
      const startDate = today.toISOString().split('T')[0];
      const nextWeek = new Date();
      nextWeek.setDate(today.getDate() + 7);
      const endDate = nextWeek.toISOString().split('T')[0];

      const translations = {
        title: { en: challenge.title },
        description: { en: challenge.description },
        evaluationCriteria: challenge.evaluationCriteria.map(criteria => ({
          criteria: { en: criteria.criteria },
          maxPoints: criteria.maxPoints,
        })),
      };

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

// Function to generate summary
exports.generateSummary = functions.https.onRequest(async (req, res) => {
  try {
    const { genAI, db, storage } = await initialize();
    const translate = new Translate();

    const generationConfig = {
      temperature: 1,
      topP: 0.95,
      topK: 64,
      maxOutputTokens: 8192,
      responseMimeType: "text/plain",
    };

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


// Evaluate user submissions
exports.evaluateSubmission = functions.firestore.document('challenge_entries/{entryId}')
  .onCreate(async (snap, context) => {
    try {
      const { genAI, db, storage } = await initialize();

      const generationConfigForEvaluation = {
        temperature: 1,
        topP: 0.95,
        topK: 64,
        maxOutputTokens: 8192,
        responseMimeType: "application/json",
      };

      const modelForEvaluation = genAI.getGenerativeModel({
        model: "gemini-1.5-flash",
        systemInstruction: "Easy to understand.",
        generationConfig: generationConfigForEvaluation,
      });

      const entry = snap.data();
      const challengeId = entry.challenge_id;
      const entryDescription = entry.entry_description;
      const photoUrls = entry.photo_urls;
      const localLanguage = entry.localLanguage;
      const localLanguageCode = entry.localLanguageCode;

      const challengeDoc = await db.collection('challenges').doc(challengeId).get();
      if (!challengeDoc.exists) {
        throw new Error('Challenge not found');
      }
      const challenge = challengeDoc.data();
      // Get English content
      const englishTitle = challenge.title['en'];
      const englishDescription = challenge.description['en'];
      const englishEvaluationCriteria = challenge.evaluationCriteria;

      let prompt = `Evaluate the following user submission based on the challenge details:
      Challenge Title: ${englishTitle}
      Description: ${englishDescription}`;

      prompt += `\nUser Submission Description: ${entryDescription}
      User Submission Images: (Attached images)

      Instructions:
      1. Verify Relevance: Carefully check if the attached images are relevant to the challenge description and user entry. The images should clearly relate to challenge description and user entry.
      2. Evaluate Each Criterion: \n`;

      englishEvaluationCriteria.forEach((criteria, index) => {
        prompt += `(${index + 1}) ${criteria.criteria.en}: max ${criteria.maxPoints} points\n`;
      });

      prompt += `\n 3. Provide Scores: Assign scores for each above mentioned criterion based on the relevance and quality of the submission. Be strict in assigning scores.
                 4. Total Score and Summary: Calculate the total score by summing up the individual scores for above mentioned criterion. Write a short evaluation summary highlighting the strengths and weaknesses of the submission, especially in relation to the images provided. Be polite and encouraging in summary.
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

      // Log the prompt to the console
      console.log('Generated Prompt:', prompt);

      const imageParts = [];
      for (let i = 0; i < photoUrls.length; i++) {
        const imagePath = await downloadImage(photoUrls[i]);
        imageParts.push(fileToGenerativePart(imagePath, "image/jpeg"));
      }

      const result = await modelForEvaluation.generateContent([prompt, ...imageParts]);
      const evaluationResponse = await result.response.text();
      const evaluation = JSON.parse(evaluationResponse);

      console.log('Generated evaluation:', evaluation);

//      if (!evaluation.scores || !evaluation.totalScore || !evaluation.summary) {
//        throw new Error('Evaluation response is incomplete', evaluation);
//      }
try{
      await snap.ref.update({
        scores: evaluation.scores,
        totalScore: evaluation.totalScore,
        summary: evaluation.summary,
        evaluatedAt: admin.firestore.FieldValue.serverTimestamp()
      });

      console.log('Submission evaluated successfully');
      }catch (e){
      console.error(e);
      }
    } catch (error) {
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

async function downloadImage(url) {
try{
const fileName = url.split('/').pop().split('?')[0]; // Extract file name from URL
      //const destFileName = path.join(__dirname, 'tmp', fileName);
      const destFileName = path.join('/tmp', fileName);
      console.log(`Downloading from URL ${url} to ${destFileName}`);
      // Create the tmp directory if it doesn't exist
      if (!fs.existsSync(path.dirname(destFileName))) {
        fs.mkdirSync(path.dirname(destFileName));
      }
      const response = await axios({
        url,
        method: 'GET',
        responseType: 'stream'
      });
      response.data.pipe(fs.createWriteStream(destFileName));
      return new Promise((resolve, reject) => {
        response.data.on('end', () => {
          console.log('Download successful:', destFileName);
          resolve(destFileName);
        });
        response.data.on('error', err => {
          console.error('Error downloading image:', err);
          reject(err);
        });
      });
    } catch (error) {
      console.error('Error downloading image:', error);
      throw error;
    }
}


//async function translateDocuments() {
exports.translateDocuments = functions.https.onRequest(async (req, res) => {
  const sdgCollection = db.collection('sdg');

  // Get all documents in 'sdg' collection
  const sdgDocs = await sdgCollection.get();
   const translate = new Translate();

  for (const doc of sdgDocs.docs) {
    const sdgId = doc.id;

    const detailsCollection = doc.ref.collection('details');
    const detailsDocs = await detailsCollection.get();

    for (const detailDoc of detailsDocs.docs) {
      const detailId = detailDoc.id;
      const data = detailDoc.data();

      // Get title and body in English
      const title = data.title.en;
      const body = data.body.en;

      // Define target languages
      const targetLanguages = ['es', 'fr', 'hi', 'ru', 'zh', 'ar'];//['hi'] ;//'es', 'fr', 'hi', 'ru', 'zh', 'ar']; // Add more language codes as needed

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

  }

  console.log('Translation process completed.');
});