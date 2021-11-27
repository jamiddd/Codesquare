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

        const receiverSnap = await this.getUserById(context.params.userId);

        if (!receiverSnap.exists) {
            return {
                response: `Document with id - ${context.params.userId} doesn't exist.`
            }
        } else {
            const registrationTokens = receiverSnap.get("registrationTokens");

            return await this.sendNotification(registrationTokens, title, body);
        }
    });
    

exports.onProjectLiked = functions.https.onCall( async (data, context) => {

    if (context.auth == null) {
		return {
			reponse: "Permission denied. Request from a client app."
		};
	}

    const projectCreatorId = data["creatorId"];
    const senderId = data["senderId"];
    const title = data["title"];

    const projectCreatorSnap = await this.getUserById(projectCreatorId);

    if (!projectCreatorSnap.exists) {
        return {
            response: `Document with id - ${projectCreatorId} doesn't exist.`
        }
    } else {

        const registrationTokens = projectCreatorSnap.get("registrationTokens");
        const senderSnap = await this.getUserById(senderId);

        if (!senderSnap.exists) {
            return {
                response: `Document with id - ${senderId} doesn't exist.`
            }
        } else {
            const name = senderSnap.get("name");
            const msg = name + " has liked your project.";
            return this.sendNotification(registrationTokens, title, msg);
        }
    }

});

/*
    @params
    userId
    registrationTokens
    senderName
    projectTitle

*/
exports.onCommentLiked = functions.https.onCall( async (data, context) => {

    if (context.auth == null) {
		return {
			reponse: "Permission denied. Request from a client app."
		};
	}

    const receiverId = data["userId"];

    const receiverSnap = await this.getUserById(receiverId);

    if (!receiverSnap.exists) {
        return {
            response: `Document with id - ${receiverId} doesn't exist.`
        }
    } else {

        const registrationTokens = receiverSnap.get("registrationTokens");

        const senderName = data["senderName"];
        const projectTitle = data["projectTitle"];

        const msg = senderName + " liked your comment";

        return this.sendNotification(registrationTokens, projectTitle, msg);
    }
});

exports.onCommentPosted = functions.firestore.document("commentChannels/{commentChannelId}/comments/{commentId}")
    .onCreate( async (snap, context) => {

        const projectId = snap.get("projectId");
        const projectSnap = await this.getProjectById(projectId);
        
        if (!projectSnap.exists) {
            return {
                response: `Document with id - ${projectId} doesn't exist.`
            };
        } else {
            const senderId = snap.get("senderId");

            const userSnap = await this.getUserById(senderId);

            if (!userSnap.exists) {
                return {
                    response: `Document with id - ${senderId} doesn't exist.`
                };   
            } else {
                const commentLevel = snap.get("commentLevel");

                if (commentLevel == 0) {
                    
                    const title = projectSnap.get("title");
                    const msg = userSnap.get("name") + " has commented on your post.";
                
                    return this.sendNotification(userSnap.get("registrationTokens"), title, msg);
                
                } else {
                    
                    const title = projectSnap.get("title");
                    const msg = userSnap.get("name") + " has replied to your comment.";

                    return this.sendNotification(userSnap.get("registrationTokens"), title, msg);
                
                }
            }
        }
    });



exports.onProjectRequestCreated = functions.firestore.document("projectRequests/{projectRequestId}")
    .onCreate( async (snap, context) => {
        const receiverId = snap.get("receiverId");
        const senderId = snap.get("senderId");
        const projectId = snap.get("projectId");

        const receiverSnap = await this.getUserById(receiverId);

        if (!receiverSnap.exists) {
            return {
                response: `Document with id - ${receiverId} doesn\'t exist.`
            };
        } else {
            const registrationToken = receiverSnap.get("registrationTokens");
            const senderSnap = await this.getUserById(senderId);

            if (!senderSnap.exists) {
                return {
                    response: `Document with id - ${senderId} doesn\'t exist.`
                };
            } else {

                const projectSnap = await this.getProjectById(projectId);

                if (!projectSnap.exists){
                    return {
                        response: `Document with id - ${projectId} doesn\'t exist.`
                    };
                } else {
                    const title = projectSnap.get("title");
                    const msg = `${senderSnap.get("name")} has requested to join your project.`;

                    return this.sendNotification(registrationToken, title, msg);
                }
            }
        }
    });


exports.onProjectRequestRejected = functions.https.onCall( async (data, context) => {

    if (context.auth == null) {
		return {
			reponse: "Permission denied. Request from a client app."
		};
	}

    const senderId = data["senderId"];
    const projectTitle = data["projectTitle"];

    const senderSnap = await this.getUserById(senderId);

    if (!senderSnap.exists) {
        return {
            response: `Document with id - ${senderId} doesn\'t exist.`
        };
    } else {
        const registrationTokens = senderSnap.get("registrationTokens");

        return this.sendNotification(registrationTokens, projectTitle, `You request to join ${projectTitle} have been accepted.`)
    }
});


exports.onContributorAdded = functions.https.onCall( async (data, context) => {

    if (context.auth == null) {
		return {
			reponse: "Permission denied. Request from a client app."
		};
	}


    const contributorId = data["contributorId"];
    const projectTitle = data["projectTitle"];

    const contributorSnap = await this.getUserById(contributorId);

    if (!contributorSnap.exists) {
        return {
            response: `Document with id - ${contributorId} doesn\'t exist.`
        };
    } else {
        const registrationTokens = contributorSnap.get("registrationTokens");

        return this.sendNotification(registrationTokens, projectTitle, `You request to join ${projectTitle} have been accepted.`)
    }
});


exports.onUserLiked = functions.https.onCall( async (data, context) => {
    const receiverId = data["receiverId"];
    const senderName = data["senderName"];

    const receiverSnap = await this.getUserById(receiverId);

    if (!receiverSnap.exists) {
        return {
            response: `Document with id - ${contributorId} doesn\'t exist.`
        };
    } else {
        const receiverRegistrationTokens = receiverSnap.get("registrationTokens");

        const title = "New Like"
        const msg = senderName + " has liked you.";

        return await this.sendNotification(receiverRegistrationTokens, title, msg);
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

                if (type == "image") {
                    return await this.sendNotificationToTopic(chatChannelId, {
                        title: projectTitle,
                        content: senderName + ": Document"
                    })
                } else if (type == "document") {
                    return await this.sendNotificationToTopic(chatChannelId, {
                        title: projectTitle,
                        content: senderName + ": Image"
                    });
                } else {
                    return await this.sendNotificationToTopic(chatChannelId, {
                        title: projectTitle,
                        content: senderName + ": " + snap.get("content") 
                    });
                }
            }
        }
    });


/**
 * 
 * @param {string[]|string} userRegistrationTokens Ids of the device to which this notificaiton will be sent
 * @param {string} notificationTitle The title of the notification
 * @param {string} notificationMsg The content of the notification
 */
exports.sendNotification = async (userRegistrationTokens, notificationTitle, notificationMsg) => {

    const payload = {
        notification: {
          title: notificationTitle,
          body: notificationMsg,
          sound: 'default'
        }
    };
    
    return await admin.messaging().sendToDevice(userRegistrationTokens, payload, {priority: 'high'});
}



/**
 * 
 * @param {string} topic The topic to which the notification should be sent to
 * @param {{title:string, content:string, img:string?}} data Data object that contains the tile, content and image of the notification
 */
exports.sendNotificationToTopic = async (topic, data) => {
	const notificationTitle = data.title;
	const msg = data.content;
	
	var payload = {};

	if (data.hasOwnProperty("img")) {
		payload = {
			notification: {
			  title: notificationTitle,
			  body: msg,
			  image: data.img,
			  imageUrl: data.img, 
			  sound: 'default'
			},
			data: {
				image: data.img
			}
		};
	} else {
		payload = {
			notification: {
			  title: notificationTitle,
			  body: msg,
			  sound: 'default'
			}
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