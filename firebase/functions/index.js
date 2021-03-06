const functions = require("firebase-functions");
const admin = require("firebase-admin");
const algoliasearch = require("algoliasearch");


admin.initializeApp();
    
const ALGOLIA_ID = functions.config().algolia.app_id;
const ALGOLIA_ADMIN_KEY = functions.config().algolia.api_key;
const ALGOLIA_SEARCH_KEY = functions.config().algolia.search_key;

const ALGOLIA_PROJECTS_INDEX = "projects";
const ALGOLIA_USERS_INDEX = "users";
const client = algoliasearch(ALGOLIA_ID, ALGOLIA_ADMIN_KEY);

class Notification {
    id = "";
    title = "";
    content = "";
    createdAt = 0;
    senderId = "";
    receiverId = "";
    image = "";
    projectId = "";
    commentChannelId = "";
    commentId = "";
    userId = "";
    type = 0;

    /**
     * 
     * @param {functions.firestore.QueryDocumentSnapshot} snapshot The firestore document that holds the notification values
     */
    constructor(snapshot) {
        this.id = snapshot.get("id");
        this.title = snapshot.get("title");
        this.content = snapshot.get("content");
        this.createdAt = snapshot.get("createdAt");
        this.senderId = snapshot.get("senderId");
        this.receiverId = snapshot.get("receiverId");
        this.image = snapshot.get("image");
        this.projectId = snapshot.get("projectId");
        this.commentChannelId = snapshot.get("commentChannelId");
        this.commentId = snapshot.get("commentId");
        this.userId = snapshot.get("userId");
        this.type = snapshot.get("type");
    }
    
}

exports.onProjectCreated = functions.firestore.document("projects/{projectId}")
    .onCreate(async (snap, context) => {
        
        const projectDocument = snap.data();

        const projectMinimal = {
            objectID: context.params.projectId,
            type: "project",
            name: projectDocument.get("name"),
            content: projectDocument.get("content"),
            createdAt: projectDocument.get("createdAt"),
            creator: projectDocument.get("creator"),
            images: projectDocument.get("images"),
            location: projectDocument.get("location"),
            tags: projectDocument.get("tags"),
            updatedAt: projectDocument.get("updatedAt")
        };
        
      
        const index = client.initIndex(ALGOLIA_PROJECTS_INDEX);
        const index1 = client.initIndex("interests");

        var tags = [];
        tags = projectMinimal.tags;
        const tagsSize = tags.length;
        var objects = [];
        if (tagsSize != 0) {
            for (let i = 0; i < tagsSize; i++) {
                const s = tags[i];
                const id = s.toLowerCase().split(' ').join('_');

                objects.push({
                    objectID: id,
                    interest: s
                });
            }

            await index1.saveObjects(objects);

        }

        return await index.saveObject(projectMinimal);
    });

exports.onProjectDeleted = functions.firestore.document("projects/{projectId}")
    .onDelete((snap, context) => {
        const index = client.initIndex(ALGOLIA_PROJECTS_INDEX);
        return index.deleteObject(context.params.projectId);
    });


exports.onUserCreated = functions.firestore.document("users/{userId}")
    .onCreate(async (snap, context) => {
        
        const userDocument = snap.data();

        const userMinimal = {
            objectID: context.params.userId,
            email: userDocument.get("email"),
            about: userDocument.get("about"),
            createdAt: userDocument.get("createdAt"),
            interests: userDocument.get("interests"),
            location: userDocument.get("location"),
            name: userDocument.get("name"),
            photo: userDocument.get("photo"),
            premiumState: userDocument.get("premiumState"),
            tag: userDocument.get("tag"),
            username: userDocument.get("username"),
            type: "user"
        };

        const index = client.initIndex(ALGOLIA_USERS_INDEX);
        const index1 = client.initIndex("interests");
        
        var interests = [];
        interests = userMinimal.interests;
        const interestsSize = interests.length;
        var objects = [];
        if (interestsSize != 0) {
            for (let i = 0; i < interestsSize; i++) {
                const s = interests[i];
                const id = s.toLowerCase().split(' ').join('_');

                objects.push({
                    objectID: id,
                    interest: s
                });
            }

            await index1.saveObjects(objects);

        }
 
        return await index.saveObject(userMinimal);
    });


exports.onUserDeleted = functions.firestore.document("users/{userId}")
    .onDelete((snap, context) => {
        const index = client.initIndex(ALGOLIA_USERS_INDEX);
    
        return index.deleteObject(context.params.userId);
    });

exports.getUserById = async (userId) => {
    return await admin.firestore()
        .collection("users")
        .doc(userId)
        .get();
};

exports.getProjectById = async (projectId) => {
    return await admin.firestore()
        .collection("projects")
        .doc(projectId)
        .get();
};


exports.getChatChannelById = async (channelId) => {
    return await admin.firestore()
        .collection("chatChannels")
        .doc(channelId)
        .get();
}

exports.onNewChannelNotification = functions.firestore.document("chatChannels/{chatChannelId}/notifications/{notificationId}")
    .onCreate( async (snap, context) => {

        var notification = new Notification(snap);

        var notificationType = ""
        if (notification.type > 0) {
            notificationType = "request"
        } else if(notification.type < 0) {
            notificationType = "invite"
        } else {
            notificationType = "general"
        }

        var data = {
            title: notification.title,
            content: notification.content,
            senderId: notification.senderId,
            receiverId: notification.receiverId,
            notificationId: notification.id,
            type: notificationType
        };

        const chatChannelSnap = await this.getChatChannelById(context.params.chatChannelId);

        if (!chatChannelSnap.exists) {
            return {
                response: `Document with id - ${chatChannelId} doesn\'t exist.`
            };
        } else {
            const registrationTokens = chatChannelSnap.get("tokens");
            if (registrationTokens.length > 0) {
                return await this.sendNotificationToTopic(context.params.chatChannelId, data);
            } else {
                return {
                    response: `No registration tokens found for user: ${context.params.userId}`
                }
            }
        }
    });

exports.onNewNotification = functions.firestore.document("users/{userId}/notifications/{notificationId}")
    .onCreate( async (snap, context) => {

        var notification = new Notification(snap);

        var notificationType = notification.type.toString();

        var data = {
            title: notification.title,
            content: notification.content,
            senderId: notification.senderId,
            receiverId: notification.receiverId,
            notificationId: notification.id,
            type: notificationType
        };

        if (notification.projectId) {
            data.projectId = notification.projectId;
        }

        if (notification.commentChannelId) {
            data.commentChannelId = notification.commentChannelId;
        }

        if (notification.userId) {
            data.userId = notification.userId;
        }

        if (notification.commentId) {
            data.commentId = notification.commentId;
        }

        const receiverSnap = await this.getUserById(context.params.userId);

        if (!receiverSnap.exists) {
            return {
                response: `Document with id - ${context.params.userId} doesn't exist.`
            }
        } else {
            const registrationToken = receiverSnap.get("token");
            if (registrationToken) {
                return await this.sendNotification(registrationToken, data);
            } else {
                return {
                    response: `No registration tokens found for user: ${context.params.userId}`
                }
            }
        }
    });
    


exports.onNewMessage = functions.firestore.document("chatChannels/{chatChannelId}/messages/{messageId}")
    .onCreate(async (snap, context) => {
        const chatChannelId = context.params.chatChannelId;
        const chatChannelSnap = await this.getChatChannelById(chatChannelId);

        const senderId = snap.get("senderId");
        const senderName = snap.get("sender.name");

        if (!chatChannelSnap.exists) {
            return {
                response: `Document with id - ${chatChannelId} doesn\'t exist.`
            };
        } else {
            const type = snap.get("type");
            const projectTitle = chatChannelSnap.get("projectTitle");

            var channelRegistrationTokens = [];
            channelRegistrationTokens = chatChannelSnap.get("tokens");

            console.log("Channel registration tokens => " + channelRegistrationTokens);

            var data = {
                title: projectTitle,
                senderId: senderId,
                channelId: chatChannelId,
            };
                
            if (type == "image") {
                data.content = senderName + ": Image";
            } else if (type == "document") {
                data.content = senderName + ": Document";
            } else {
                data.content = senderName + ": " + snap.get("content");
            }

            const result = await this.sendNotification(channelRegistrationTokens, data);

            const staleTokens = result.results.reduce((acc, cur, idx) => {
                if (cur.error && cur.error.code === 'messaging/registration-token-not-registered') {
                    acc.push(channelRegistrationTokens[idx])
                }
                return acc
            }, [])
                   
            if (staleTokens.length > 0) {
                // await deleteSessionForDevices(staleTokens, pgdb)
                // debug('deleted sessions for stale firebase device tokens', staleTokens)
            
                const goodTokens = channelRegistrationTokens.filter(t => staleTokens.indexOf(t) === -1);
            
                return await admin.firestore().collection("chatChannels").doc(chatChannelId).update({tokens: goodTokens});
            } else {
                return {
                    response: "Successfully sent message to chat channel with no stale tokens."
                }
            }
        }
    });


/**
 * 
 * @param {string[]|string} userRegistrationTokens Ids of the device to which this notificaiton will be sent
 * @param {string} notificationTitle The title of the notification
 * @param {string} notificationMsg The content of the notification
 * @param {string} senderId The id of the sender who sent this notification
 * @param {string} deepLink Deeplink for android navigation
 * @param {string} clickAction Action fo android
 */
exports.sendNotification = async (userRegistrationTokens, dataObject) => {

    const payload = {
        notification: {
          title: dataObject.title,
          body: dataObject.content,
          sound: 'default',
        },
        data: dataObject
    };
    
    return await admin.messaging().sendToDevice(userRegistrationTokens, payload, {priority: 'high'});
}



/**
 * 
 * @param {string} topic The topic to which the notification should be sent to
 * @param {any} dataObject Data object that contains the tile, senderId, content, image and deepLink of the notification
 */
exports.sendNotificationToTopic = async (topic, dataObject) => {	
	var payload = {};

	if (dataObject.hasOwnProperty("img")) {
		payload = {
			notification: {
			  title: dataObject.title,
			  body: dataObject.content,
			  image: dataObject.img,
			  imageUrl: dataObject.img, 
			  sound: 'default',
			},
			data: dataObject
		};
	} else {
		payload = {
			notification: {
			  title: dataObject.title,
			  body: dataObject.content,
			  sound: 'default',
			},
            data: dataObject
		};
	}

	// Send a message to devices subscribed to the provided topic.
	try {
		return await admin.messaging().sendToTopic(topic, payload, { priority: 'high' });
	} catch (error) {
		return {
            response: `Error sending message:, ${error}`
        }
	}
}


exports.onCommentDeleted = functions.firestore.document("commentChannels/{commentChannelId}/comments/{commentId}")
    .onDelete(async (snap, context) => {
        const threadChannelId = snap.get("threadChannelId");

        return await admin.firestore()
            .collection("commentChannels")
            .doc(threadChannelId)
            .delete();
    });


exports.onCommentChannelDeleted = functions.firestore.document("commentChannels/{commentChannelId}")
    .onDelete(async (snap, context) => {
        const documents = await snap.ref.collection("comments").listDocuments();
    
        const batch = admin.firestore().batch();
        documents.forEach((doc, i) => {
            batch.delete(doc);
        });

        return await batch.commit();
    });


exports.onProjectUpdated = functions.firestore.document("projects/{projectId}")
    .onUpdate(async (change, context) => {
        const projectDocument = change.after;

        // check if changes are not relevant to algolia
        if (
            previousDocument.get("name") == userDocument.get("name") &&
            previousDocument.get("content") == userDocument.get("content") &&
            previousDocument.get("tags") == userDocument.get("tags") &&
            previousDocument.get("images") == userDocument.get("images") &&
            previousDocument.get("creator") == userDocument.get("creator") && 
            previousDocument.get("location") == userDocument.get("location")
        ) {
            return;
        }

        const projectMinimal = {
            objectID: context.params.projectId,
            type: "project",
            name: projectDocument.get("name"),
            content: projectDocument.get("content"),
            createdAt: projectDocument.get("createdAt"),
            creator: projectDocument.get("creator"),
            images: projectDocument.get("images"),
            location: projectDocument.get("location"),
            tags: projectDocument.get("tags"),
            updatedAt: projectDocument.get("updatedAt")
        };

        const index = client.initIndex("projects");

        return await index.partialUpdateObjects([projectMinimal], {
            createIfNotExists: true,
        });

    });

exports.onUserUpdated = functions.firestore.document("users/{userId}")
    .onUpdate(async (change, context) => {
        
        const previousDocument = change.before;
        
        const userDocument = change.after;

        // check if changes are not relevant to algolia
        if (
            previousDocument.get("name") == userDocument.get("name") &&
            previousDocument.get("about") == userDocument.get("about") &&
            previousDocument.get("tag") == userDocument.get("tag") &&
            previousDocument.get("photo") == userDocument.get("photo") &&
            previousDocument.get("username") == userDocument.get("username") && 
            previousDocument.get("interests") == userDocument.get("interests")
        ) {
            return;
        }


        const userMinimal = {
            objectID: context.params.userId,
            email: userDocument.get("email"),
            about: userDocument.get("about"),
            createdAt: userDocument.get("createdAt"),
            interests: userDocument.get("interests"),
            location: userDocument.get("location"),
            name: userDocument.get("name"),
            photo: userDocument.get("photo"),
            premiumState: userDocument.get("premiumState"),
            tag: userDocument.get("tag"),
            username: userDocument.get("username"),
            type: "user"
        };

        const index = client.initIndex("users");
        const index1 = client.initIndex("interests");

        var interests = [];
        interests = userMinimal.interests;
        const interestsSize = interests.length;
        var objects = [];
        if (interestsSize != 0) {
            for (let i = 0; i < interestsSize; i++) {
                const s = interests[i];
                const id = s.toLowerCase().split(' ').join('_');

                objects.push({
                    objectID: id,
                    interest: s
                });
            }

            await index1.saveObjects(objects);

        }

        return await index.partialUpdateObjects([userMinimal], {
            createIfNotExists: true,
          });

    });

exports.verifyPurchase = functions.https.onCall(async (data, context) => {
    var purchaseInfo = {
        purchaseToken: data.purchaseToken,
        orderId: data.purchaseOrderId,
        purchaseTime: data.purchaseTime,
        productId: data.productId,
        isValid: false
    };

    const userId = data.userId;

    const docSnap = await admin.firestore()
        .doc(`users/${userId}/purchases/${purchaseInfo.purchaseToken}`)
        .get();
        
    if (docSnap.exists) {
        return purchaseInfo;
    } else {
        purchaseInfo.isValid = true;

        await admin.firestore()
                .doc(`users/${userId}/purchases/${purchaseInfo.purchaseToken}`)
                .set(purchaseInfo);
            
        return purchaseInfo;
    }
});



    // NOTIFICATIONS
    // ------------------
    // ---on request sent/undo--
    // on request accept/reject
    
    // ---on comment/reply posted---
    // ---on project liked---
    // ---on new contributor joined---
    // on user liked/disliked
    // promotion 
    // ---chat messages---