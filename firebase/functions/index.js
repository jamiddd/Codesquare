const functions = require("firebase-functions");
const admin = require("firebase-admin");
const algoliasearch = require("algoliasearch");

admin.initializeApp();

const ALGOLIA_ID = functions.config().algolia.app_id;
const ALGOLIA_ADMIN_KEY = functions.config().algolia.api_key;
const ALGOLIA_SEARCH_KEY = functions.config().algolia.search_key;

const ALGOLIA_INDEX_NAME = "projects";
const client = algoliasearch(ALGOLIA_ID, ALGOLIA_ADMIN_KEY);


exports.onProjectCreated = functions.firestore.document("projects/{projectId}")
    .onCreate((snap, context) => {

        console.log(ALGOLIA_ID + " --- " + ALGOLIA_ADMIN_KEY)

        console.log("Hello World")

        const project = snap.data();
        project.objectID = context.params.projectId;
        const index = client.initIndex(ALGOLIA_INDEX_NAME);

        return index.saveObject(project);
    });
