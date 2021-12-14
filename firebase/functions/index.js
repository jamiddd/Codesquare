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


exports.onProjectCreated = functions.firestore.document("projects/{projectId}")
    .onCreate((snap, context) => {
        const project = snap.data();
        project.objectID = context.params.projectId;
        project.type = "project"
        const index = client.initIndex(ALGOLIA_PROJECTS_INDEX);

        return index.saveObject(project);
    });

exports.onProjectDeleted = functions.firestore.document("projects/{projectId}")
    .onDelete((snap, context) => {
        const index = client.initIndex(ALGOLIA_PROJECTS_INDEX);
        return index.deleteObject(context.params.projectId);
    });


exports.onUserCreated = functions.firestore.document("users/{userId}")
    .onCreate((snap, context) => {
        const user = snap.data();
        user.objectID = context.params.userId;
        user.type = "user"
        const index = client.initIndex(ALGOLIA_USERS_INDEX);
        return index.saveObject(user);
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


exports.onNewNotification = functions.firestore.document("users/{userId}/notifications/{notificationId}")
    .onCreate( async (snap, context) => {

        const title = snap.get("title");
        const body = snap.get("content");
        const senderId = snap.get("senderId");

        const notificationType = snap.get("type");
        const contextId = snap.get("contextId");

        var data = {
            title: title,
            content: body,
            senderId: senderId,
            contextId: contextId,
            receiverId: context.params.userId,
            notificationId: context.params.notificationId
        };

        if (notificationType == 11) {
            data.deepLink = "www.collab.com/projectRequests"
            data.clickAction = "OPEN_PROJECT_REQUESTS"
            data.type = "request"
        } else {
            data.deepLink = "www.collab.com/notifications"
            data.clickAction = "OPEN_NOTIFICATIONS"
            data.type = "notification"
        }

        const receiverSnap = await this.getUserById(context.params.userId);

        if (!receiverSnap.exists) {
            return {
                response: `Document with id - ${context.params.userId} doesn't exist.`
            }
        } else {
            const registrationTokens = receiverSnap.get("registrationTokens");

            return await this.sendNotification(registrationTokens, title, body, data);
        }
    });
    


exports.onNewMessage = functions.firestore.document("chatChannels/{chatChannelId}/messages/{messageId}")
    .onCreate(async (snap, context) => {
        const chatChannelId = context.params.chatChannelId;
        const chatChannelSnap = await this.getChatChannelById(chatChannelId);

        const senderId = snap.get("senderId");
        const senderSnap = await this.getUserById(senderId);

        if (!chatChannelSnap.exists) {
            return {
                response: `Document with id - ${chatChannelId} doesn\'t exist.`
            };
        } else {

            if (!senderSnap.exists) {
                return {
                    response: `Document with id - ${senderId} doesn\'t exist.`
                };
            } else {
                const type = snap.get("type");
                const projectTitle = chatChannelSnap.get("projectTitle");

                const senderName = senderSnap.get("name");
                const senderRegistrationTokens = senderSnap.get("registrationTokens");

                var channelRegistrationTokens = [];
                channelRegistrationTokens = chatChannelSnap.get("registrationTokens");

                const finalTokens = channelRegistrationTokens.filter(t => senderRegistrationTokens.indexOf(t) === -1);

                var data = {
                    title: projectTitle,
                    senderId: senderId,
                    channelId: chatChannelId,
                    deepLink: "www.collab.com/chats/" + chatChannelId,
                    clickAction: "OPEN_CHATS"
                };
                
                if (type == "image") {
                    data.content = senderName + ": Image";
                } else if (type == "document") {
                    data.content = senderName + ": Document";
                } else {
                    data.content = senderName + ": " + snap.get("content");
                }

                const result = await this.sendNotification(finalTokens, data);

                const staleTokens = result.results.reduce((acc, cur, idx) => {
                    if (cur.error && cur.error.code === 'messaging/registration-token-not-registered') {
                        acc.push(finalTokens[idx])
                    }
                    return acc
                }, [])
                   
                if (staleTokens.length > 0) {
                    // await deleteSessionForDevices(staleTokens, pgdb)
                    // debug('deleted sessions for stale firebase device tokens', staleTokens)
            
                    const goodTokens = channelRegistrationTokens.filter(t => staleTokens.indexOf(t) === -1);

                    return await admin.firestore.collection("chatChannels").doc(chatChannelId).update({registrationTokens: goodTokens});
                } else {
                    return {
                        response: "Successfully sent message to chat channel with no stale tokens."
                    }
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
          click_action: dataObject.clickAction
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
              click_action: dataObject.clickAction
			},
			data: dataObject
		};
	} else {
		payload = {
			notification: {
			  title: dataObject.title,
			  body: dataObject.content,
			  sound: 'default',
              click_action: dataObject.clickAction
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