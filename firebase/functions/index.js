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
        const project = snap.data();
        project.objectID = context.params.projectId;
        const index = client.initIndex(ALGOLIA_INDEX_NAME);

        return index.saveObject(project);
    });

exports.onProjectDeleted = functions.firestore.document("projects/{projectId}")
    .onDelete((snap, context) => {
        const index = client.initIndex("projects");
        return index.deleteObject(context.params.projectId);
    });


exports.onUserCreated = functions.firestore.document("users/{userId}")
    .onCreate((snap, context) => {
        const user = snap.data();
        user.objectID = context.params.userId;
        const index = client.initIndex("users");
        return index.saveObject(user);
    });


exports.onUserDeleted = functions.firestore.document("users/{userId}")
    .onDelete((snap, context) => {
        const index = client.initIndex("users");
    
        return index.deleteObject(context.params.userId);
    });


    // NOTIFICATIONS
    // ------------------
    // on request sent/undo
    // on request accept/reject
    // on comment/reply posted
    // on project liked
    // on new contributor joined
    // on user liked/disliked
    // promotion 