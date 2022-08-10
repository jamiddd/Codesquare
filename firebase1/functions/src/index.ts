import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import * as algoliasearch from "algoliasearch";
import { PostMinimal } from "./data/PostMinimal";
import * as Constants from "./extras/Constants";
import { UserMinimal2 } from "./data/UserMinimal2";
import { MyNotification } from "./data/MyNotification";
import { NotificationPayload } from "./data/NotificationPayload";
import { MessagingDevicesResponse } from "firebase-admin/messaging";
import { chatChannelConverter, convertSnapshotToComment, convertSnapshotToCommentChannel, convertSnapshotToInterestItem, convertSnapshotToMessage, convertSnapshotToNotification, convertSnapshotToPost, convertSnapshotToPostMinimal, convertSnapshotToUserMinimal, getPurchaseInfoFromObject, userConverter } from "./extras/Utilities";
import { FieldValue, Query } from "firebase-admin/firestore";
import { InterestItem } from "./data/InterestItem";
import { UserMinimal } from "./data/UserMinimal";
// import * as spawn from "child-process-promise";
// import * as path from "path";
// import * as os from "os";
// import * as fs from "fs";
// import { uuid } from "uuidv4";
// import { getStorage, ref, getDownloadURL } from "firebase/storage";

admin.initializeApp();

const ALGOLIA_ID = functions.config().algolia.app_id;
const ALGOLIA_ADMIN_KEY = functions.config().algolia.api_key;
const ALGOLIA_POSTS_INDEX = Constants.POSTS;
const ALGOLIA_USERS_INDEX = Constants.USERS;
const ALGOLIA_INTERESTS_INDEX = Constants.INTERESTS;

const client = algoliasearch.default(ALGOLIA_ID, ALGOLIA_ADMIN_KEY);


/**
 * Post related function starts here
 * 
 * 1. On create post
 * 2. On update post
 * 3. On delete post
 * 
 */

const postPath = "posts/{postId}";

export const onPostCreated = functions.firestore.document(postPath).onCreate(async (snap, _) => {
    const postMinimal: PostMinimal = convertSnapshotToPostMinimal(snap);
    const postIndex = client.initIndex(ALGOLIA_POSTS_INDEX);

    const response = await addInterests(postMinimal.tags);
    console.log(response);

    return await postIndex.saveObject(postMinimal);
});


export const isCriticalPostChange = (before: PostMinimal, after: PostMinimal) => {
    if (before.name == after.name &&
        before.content == after.content &&
        before.tags == after.tags &&
        before.mediaList == after.mediaList &&
        before.creator == after.creator &&
        before.location == after.location &&
        before.rank == after.rank) {
        return false;
    } else {
        return true;
    }
};

/**
 * 
 * 1. algolia
 * 2. update chatchannel based on the post
 * 
 */
export const onPostUpdated = functions.firestore.document(postPath).onUpdate(async (change, _context) => {
    const oldP = convertSnapshotToPostMinimal(change.before);
    const newP = convertSnapshotToPostMinimal(change.after);

    if (isCriticalPostChange(oldP, newP)) {
        /* Algolia changes */
        const postIndex = client.initIndex(ALGOLIA_POSTS_INDEX);
        await postIndex.partialUpdateObjects([newP], {
            createIfNotExists: true,
        });

        /* Algolia changes end */

        /* Ranked changes */ /* To be implemented in the future */
        /* if (oldP.rank != newP.rank) {
            // lets figure out if the change is upvote or downvote
            if (newP.upvoteCount > oldP.upvoteCount) {
                const upperRankedPostList = await admin.firestore()
                    .collection("posts")
                    .where("rank", ">", newP.rank)
                    .limit(1)
                    .get();

                if (upperRankedPostList.empty) {
                    // rank 1
                    return {
                        response: "Upper ranked post list empty."
                    };
                } else {
                    const topPost = convertSnapshotToPost(upperRankedPostList.docs[0]);

                    if (topPost.upvoteCount < newP.upvoteCount) {
                        // switch rank

                        const now = Date.now();

                        const batch = admin.firestore().batch();

                        const currentPostRef = admin.firestore().collection("posts").doc(newP.id);
                        const topPostRef = admin.firestore().collection("posts").doc(topPost.id);

                        batch.update(currentPostRef, {
                            rank: topPost.rank,
                            updatedAt: now
                        });

                        batch.update(topPostRef, {
                            rank: newP.rank,
                            updatedAt: now
                        });

                        return batch.commit();
                    } else {
                        // do nothing
                        return {
                            response: "Top post upvote count is greater than new post upvote count"
                        };
                    }
                }
            } else if (newP.upvoteCount < oldP.upvoteCount) {
                const lowerRankedPostList = await admin.firestore()
                    .collection("posts")
                    .where("rank", "<", newP.rank)
                    .limit(1)
                    .get();

                if (lowerRankedPostList.empty) {
                    // most bottom rank
                    return {
                        response: "Most bottom rank"
                    };
                } else {
                    const bottomPost = convertSnapshotToPost(lowerRankedPostList.docs[0]);

                    if (bottomPost.upvoteCount > newP.upvoteCount) {
                        // switch rank

                        const now = Date.now();

                        const batch = admin.firestore().batch();

                        const currentPostRef = admin.firestore().collection("posts").doc(newP.id);
                        const bottomPostRef = admin.firestore().collection("posts").doc(bottomPost.id);

                        batch.update(currentPostRef, {
                            rank: bottomPost.rank,
                            updatedAt: now
                        });

                        batch.update(bottomPostRef, {
                            rank: newP.rank,
                            updatedAt: now
                        });

                        return batch.commit();
                    } else {
                        // do nothing
                        return {
                            response: "Bottom post upvote count is greater than new post upvote count"
                        };
                    }
                } 
            } else {
                return {
                    response: "To implement something in the future, where for equal upvotes, what should be the next criteria to decide"
                };
            }
        } /*

        /* Ranked changes end */

        /* Chat channel changes */

        const changes = {
            postTitle: newP.name,
            postImage: newP.thumbnail,
            updatedAt: newP.updatedAt
        };


        await admin.firestore()
            .collection("chatChannels")
            .doc(newP.chatChannel)
            .update(changes);

        /* Chat channel changes end */

        const updatedData = convertSnapshotToPost(change.after);

        const changes1 = [
            {
                query: admin.firestore().collectionGroup("savedPosts")
                    .where("id", "==", newP.objectID)
                    .orderBy("createdAt", "desc"),
                updatedData: updatedData
            },
            {
                query: admin.firestore().collectionGroup("likedPosts")
                    .where("id", "==", newP.objectID)
                    .orderBy("createdAt", "desc"),
                updatedData: updatedData
            }
        ];

        changes1.forEach(async (value) => {
            await batchUpdate(value.query, value.updatedData);
        });
    
        return {
            response: "All user references updated."
        };
    } else {
        return {
            response: "No critical changes found to make changes in the database."
        };
    }
});

export const onPostDeleted = functions.firestore.document(postPath).onDelete(async (_snap, context) => {
    const postIndex = client.initIndex(ALGOLIA_POSTS_INDEX);
    return postIndex.deleteObject(context.params.postId);
});

/**
 * 
 * User related functions start here
 * 
 * 1. On create user
 * 2. On update user
 * 3. On delete user
 * 4. On new notification for a user
 */

const userPath = "users/{userId}";
const notificationPath = "notifications/{notificationId}";

export const onUserCreated = functions.firestore.document(userPath).onCreate(async (snap, _context) => {
    const userMinimal: UserMinimal2 = convertSnapshotToUserMinimal(snap);
    const userIndex = client.initIndex(ALGOLIA_USERS_INDEX);

    const response = await addInterests(userMinimal.interests);

    console.log(response);

    return await userIndex.saveObject(userMinimal);
});


export const addInterests = async (interests: string[]) => {
    try {
        await admin.firestore().runTransaction(async (transaction) => {
            for (let i = 0; i < interests.length; i++) {
                const currentInterest: string = interests[i];
                const restOfTheInterests = interests.filter((s: string, _j: number, _arr: string[]) => {
                    return s != currentInterest;
                });
        
                const now = Date.now();
        
                // we need to make sure that there are no symbols or weird shit in the interest string
                // so we need to set up check in the client itself to prevent this situation
                const id = currentInterest.toLocaleLowerCase().trim().replace(" ", "_");
        
                const interestItem: InterestItem = {
                    itemId: id,
                    objectID: id,
                    content: currentInterest,
                    createdAt: now,
                    associations: restOfTheInterests,
                    weight: 0.001, // the first increment
                    updatedAt: now
                };
                
                const interestItemRef = admin.firestore()
                    .collection(Constants.INTERESTS)
                    .doc(interestItem.itemId);

                const snap = await transaction.get(interestItemRef);
                if (snap.exists) {
                    const interestItem1 = convertSnapshotToInterestItem(snap);

                    const intersection = interestItem.associations.filter((value) => interestItem1.associations.includes(value));
                    if (intersection.length > 0) {
                        // there is something common
                        const changes = {
                            "associations": intersection,
                            "weight": FieldValue.increment(0.001),
                            "updatedAt": now
                        };

                        transaction.update(interestItemRef, changes);
                    } else {
                        // nothing is common
                        const changes = {
                            "associations": interestItem.associations, // new associations
                            "weight": FieldValue.increment(0.001),
                            "updatedAt": now
                        };

                        transaction.update(interestItemRef, changes);
                    }
                } else {
                    transaction.set(interestItemRef, interestItem);
                }
            }
        });
        
        
        return {
            response: "Successfully added or updated interest."
        };
    } catch (error) {
        return {
            response: "Something went wrong while trying to add interest."
        };
    }
};

// onInterestCreated
// addInterest

const interestPath = "interests/{itemId}";

export const onInterestCreated = functions.firestore.document(interestPath).onCreate(async (snap, context) => {
    const interestItem = convertSnapshotToInterestItem(snap);
    const interestIndex = client.initIndex(ALGOLIA_INTERESTS_INDEX);
    return await interestIndex.saveObject(interestItem);
});

export const onInterestUpdated = functions.firestore.document(interestPath).onUpdate(async (change, context) => {
    const newInterestItem = convertSnapshotToInterestItem(change.after);
    // const oldInterestItem = convertSnapshotToInterestItem(change.before);

    const interestIndex = client.initIndex(ALGOLIA_INTERESTS_INDEX);
    return await interestIndex.partialUpdateObject(newInterestItem, { createIfNotExists: true });
});

export const onInterestDelete = functions.firestore.document(interestPath).onDelete(async (snap, context) => {
    const interestIndex = client.initIndex(ALGOLIA_INTERESTS_INDEX);
    const interestItem = convertSnapshotToInterestItem(snap);
    return await interestIndex.deleteObject(interestItem.objectID);
});


export const addInterestsBySelection = async (interests: string[]) => {
    try {
        await admin.firestore().runTransaction(async (transaction) => {
            for (let i = 0; i < interests.length; i++) {
                const currentInterest = interests[i];
                const id = currentInterest.toLocaleLowerCase().trim().replace(" ", "_");
                const interestItemRef = admin.firestore().collection("interests").doc(id);

                const restOfTheInterests = interests.filter((s: string, _j: number, _arr: string[]) => {
                    return s != currentInterest;
                });
        
                const now = Date.now();        
                // we need to make sure that there are no symbols or weird shit in the interest string
                // so we need to set up check in the client itself to prevent this situation
        
                const interestItem: InterestItem = {
                    itemId: id,
                    objectID: id,
                    content: currentInterest,
                    createdAt: now,
                    associations: restOfTheInterests,
                    weight: 0.001, // the first increment
                    updatedAt: now
                };

                const snap = await transaction.get(interestItemRef);
                if (!snap.exists) {
                    // creating a doc only if it doesn't exist
                    
                    transaction.set(interestItemRef, interestItem);
                }
            }
        });

        return {
            response: "Added interests by selection."
        };
    } catch (error) {
        return {
            reponse: "Something went wrong while trying to add selective interests."
        };
    }
};

export const isCriticalUserChange = (before: UserMinimal2, after: UserMinimal2) => {
    if (before.name == after.name &&
        before.about == after.about &&
        before.tag == after.tag &&
        before.photo == after.photo &&
        before.username == after.username) {
        return false;
    } else {
        return true;
    }
};


export const batchUpdate = async (baseQuery: Query, updatedData: any) => {
    let hasReachedEnd = false;
    let lastDocument: admin.firestore.DocumentSnapshot<admin.firestore.DocumentData> | undefined;

    while (!hasReachedEnd) {
        const batch = admin.firestore().batch();
        let readItems: admin.firestore.QuerySnapshot<admin.firestore.DocumentData>;

        if (lastDocument) {
            readItems = await baseQuery
                .startAfter(lastDocument)
                .limit(500)
                .get();
        } else {
            readItems = await baseQuery
                .limit(500)
                .get();
        }
        
        readItems.forEach((doc) => {
            batch.update(doc.ref, updatedData);
        });  

        await batch.commit();

        if (readItems.size < 500) {
            hasReachedEnd = true;
        } else {
            lastDocument = readItems.docs.at(-1);
        }
    }
};


/**
 * 
 * Things to do after a user document is updated
 * 1. update in algolia
 * 2. update all posts
 * 3. update all comments
 * 4. update all messages
 * 
 */
export const onUserUpdated = functions.firestore.document(userPath).onUpdate(async (change, _context) => {
    const oldU = convertSnapshotToUserMinimal(change.before);
    const newU = convertSnapshotToUserMinimal(change.after);

    if (isCriticalUserChange(oldU, newU)) {
        /* Updating algoila document */

        const response = await addInterestsBySelection(newU.interests);
        console.log(response);

        const userIndex = client.initIndex(ALGOLIA_USERS_INDEX);
        await userIndex.partialUpdateObjects([newU], {
            createIfNotExists: true,
        });

        /* Alogolia update end */

        const userMinimal: UserMinimal = {
            userId: newU.objectID,
            name: newU.name,
            photo: newU.photo!,
            username: newU.username,
            premiumState: newU.premiumState
        };


        const changes = [
            {
                query: admin.firestore().collectionGroup("posts")
                    .where("creator.userId", "==", newU.objectID)
                    .orderBy("createdAt", "desc"),

                updatedData: {
                    creator: userMinimal
                }
            }, 
            {
                query: admin.firestore().collectionGroup("comments")
                    .where("senderId", "==", newU.objectID)
                    .orderBy("createdAt", "desc"),
                
                updatedData: {
                    sender: userMinimal
                }
            },
            {
                query: admin.firestore().collectionGroup("messages")
                    .where("senderId", "==", newU.objectID)
                    .orderBy("createdAt", "desc"),

                updatedData: {
                    sender: userMinimal
                }
            },
            {
                query: admin.firestore().collectionGroup("likedBy")
                    .where("userId", "==", newU.objectID)
                    .orderBy("createdAt", "desc"),

                updatedData: userMinimal
            }, {
                query: admin.firestore().collection("chatChannels")
                    .where("data1.userId", "==", newU.objectID)
                    .orderBy("createdAt", "desc"),
                updatedData: {
                    data1: userMinimal
                }    
            }, {
                query: admin.firestore().collection("chatChannels")
                    .where("data2.userId", "==", newU.objectID)
                    .orderBy("createdAt", "desc"),
                updatedData: {
                    data2: userMinimal
                }    
            }
        ];

        changes.forEach(async (value) => {
            await batchUpdate(value.query, value.updatedData);
        });

        return {
            response: "Updated all messages, posts and comments, and likedBys"
        };
    } else {
        return {
            response: "No critical changes were noticed. Not updating anything based on user document change"
        };
    }
});


/* TODO("all comments, posts and messages by the user should also be deleted") */
export const onUserDeleted = functions.firestore.document(userPath).onDelete(async (_snap, context) => {
    const userIndex = client.initIndex(ALGOLIA_USERS_INDEX);
    return userIndex.deleteObject(context.params.userId);
});

export const onNewNotification = functions.firestore.document(`${userPath}/${notificationPath}`).onCreate(async (snap, context) => {
    const notification: MyNotification = convertSnapshotToNotification(snap);

    const notificationPayload: Record<string, string> = {
        title: notification.title,
        content: notification.content,
        senderId: notification.senderId,
        receiverId: notification.receiverId,
        notificationId: notification.id,
        type: notification.type.toString()
    };

    if (notification.postId) {
        notificationPayload.postId = notification.postId;
    }

    if (notification.commentChannelId) {
        notificationPayload.commentChannelId = notification.commentChannelId;
    }

    if (notification.userId) {
        notificationPayload.userId = notification.userId;
    }

    if (notification.commentId) {
        notificationPayload.commentId = notification.commentId;
    }

    const receiverSnapshot = await admin.firestore()
        .collection(Constants.USERS)
        .withConverter(userConverter)
        .doc(context.params.userId)
        .get();

    if (!receiverSnapshot.exists) {
        return {
            response: `Document with id - ${context.params.userId} doesn't exist.`
        };
    } else {
        const receiver = receiverSnapshot.data();
        if (receiver) {
            if (receiver.token.length > 0) {
                return await sendNotification(receiver.token, notificationPayload);
            } else {
                return {
                    response: `No registration tokens found for user: ${context.params.userId}`
                };
            }
        } else {
            return {
                response: `Receiver with ${context.params.userId} is undefined.`
            };
        }
    }
});


/**
 * Channel related functions start here
 * 
 * 1. On channel notification
 * 2. On new message in chat
 */
const chatChannelPath = "chatChannels/{chatChannelId}";
const channelNotificationPath = chatChannelPath + "/notifications/{notificationId}";
const channelMessagesPath = chatChannelPath + "/messages/{messageId}";

export const onNewChannelNotification = functions.firestore.document(channelNotificationPath).onCreate(async (snap, context) => {
    const notification: MyNotification = convertSnapshotToNotification(snap);

    let type: string;
    if (notification.type > 0) {
        type = "request";
    } else if (notification.type < 0) {
        type = "invite";
    } else {
        type = "general";
    }

    const data: NotificationPayload = {
        title: notification.title,
        content: notification.content,
        senderId: notification.senderId,
        receiverId: notification.receiverId,
        notificationId: notification.id,
        type,
        sound: "default"
    };

    const chatChannelSnap = await admin.firestore()
        .collection(Constants.CHAT_CHANNELS)
        .withConverter(chatChannelConverter)
        .doc(context.params.chatChannelId)
        .get();


    if (!chatChannelSnap.exists) {
        return {
            response: `Document with id - ${context.params.chatChannelId} doesn't exist.`
        };
    } else {
        const chatChannel = chatChannelSnap.data();
        if (chatChannel !== undefined) {
            const tokens = chatChannel.tokens;

            if (tokens.length > 0) {
                return await sendNotificationToTopic(context.params.chatChannelId, data);
            } else {
                return {
                    response: `No registration tokens found for user: ${context.params.userId}`
                };
            }
        } else {
            return {
                response: `Chat channel is undefined : ${context.params.chatChannelId}`
            };
        }
    }
});

export const onNewMessage = functions.firestore.document(channelMessagesPath).onCreate(async (snap, context) => {
    const chatChannelId = context.params.chatChannelId;
    const message = convertSnapshotToMessage(snap);

    const chatChannelSnap = await admin.firestore()
        .collection(Constants.CHAT_CHANNELS)
        .withConverter(chatChannelConverter)
        .doc(context.params.chatChannelId)
        .get();

    if (!chatChannelSnap.exists) {
        return {
            response: `Document with id - ${chatChannelId} doesn't exist.`
        };
    } else {
        const senderSnap = await admin.firestore().collection(Constants.USERS).withConverter(userConverter).doc(message.senderId).get();
        const sender = senderSnap.data();

        const senderRegistrationToken = sender?.token;

        const chatChannel = chatChannelSnap.data();
        if (chatChannel) {
            const data: Record<string, string> = {
                title: chatChannel.postTitle,
                senderId: message.senderId,
                channelId: chatChannel.chatChannelId
            };

            if (message.type == Constants.IMAGE) {
                data.content = message.sender.name + ": Image";
            } else if (message.type == "document") {
                data.content = message.sender.name + ": Document";
            } else if (message.type == "video") {
                data.content = message.sender.name + ": Video";
            } else {
                data.content = message.sender.name + ": " + snap.get("content");
            }

            const tokens: string[] = chatChannel.tokens;

            if (senderRegistrationToken) {
                const index = tokens.indexOf(senderRegistrationToken);
                if (index !== -1) {
                    tokens.splice(index, 1);
                }                
            }

            const result = await sendNotification(tokens, data);

            const staleTokens: string[] = result.results.reduce((acc: string[], cur, index) => {
                if (cur.error && cur.error.code === "messaging/registration-token-not-registered") {
                    acc.push(tokens[index]);
                }
                return acc;
            }, []);

            if (staleTokens.length > 0) {
                // await deleteSessionForDevices(staleTokens, pgdb)
                // debug('deleted sessions for stale firebase device tokens', staleTokens)
                const goodTokens = tokens.filter((t) => staleTokens.indexOf(t) === -1);

                return await admin.firestore()
                    .collection(Constants.CHAT_CHANNELS)
                    .doc(chatChannelId)
                    .update({
                        tokens: goodTokens
                    });
            } else {
                return {
                    response: "Successfully sent message to chat channel with no stale tokens."
                };
            }
        } else {
            return {
                response: "Chat channel is undefined"
            };
        }
    }
});


/**
 * 
 * Comment related functions start here
 * 
 * 1. On single comment deleted
 * 2. on single comment channel deleted
 *  
 */

const commentChannelPath = "commentChannels/{commentChannelId}";
const commentPath = "comments/{commentId}";


export const onCommentDeleted = functions.firestore.document(`${commentChannelPath}/${commentPath}`).onDelete(async (snap, _context) => {
    /*
        When a comment is deleted there are couple of things needed to be taken care of

        1. The thread channel associated with the comment must be deleted
        2. All the comments in the thread channel comments collection must be deleted
        3. Total comments deleted [current + child_comments] need to be reduced from post.commentsCount   
        4. if the comment is directly related to post [ie. commentLevel = 0] then check if it is the last comment
        if, it is last comment, then we need to get the second last comment and update the parent comment channel
        if not then we are okay.

        while trying to get the second last comment,
        if there is no second last comment, probably the comment we deleted was the only comment

    */

    const comment = convertSnapshotToComment(snap);  
    
    // update post document
    const now = Date.now();

    const db = admin.firestore();
    const threadChannelRef = admin.firestore()
        .collection(Constants.COMMENT_CHANNELS)
        .doc(comment.threadChannelId);
  
    try {
        await db.runTransaction( async (transaction) => {
            // // get the thread channel for comments count
            // const threadChannelDoc = await transaction.get(threadChannelRef);
            // const threadChannel: CommentChannel = convertSnapshotToCommentChannel(threadChannelDoc);

            // // get total comments count
            // const totalCommentsCount = threadChannel.commentsCount + 1;

            // console.log("onCommentDelete: Total numbers of comments that were deleted = " + totalCommentsCount);

            const changes = {
                commentsCount: FieldValue.increment(-1),
                updatedAt: now
            };

            const postRef = admin.firestore().collection(Constants.POSTS)
                .doc(comment.postId);
            
            transaction.update(postRef, changes);

            // delete the thread channel 
            transaction.delete(threadChannelRef);
        });


        // update replies count of parent comment, if this comment belongs to another comment
        // if not simply ignore
        if (comment.parentCommentChannelId) {
            const parentCommentRef = admin.firestore()
                .collection(Constants.COMMENT_CHANNELS)
                .doc(comment.parentCommentChannelId)
                .collection(Constants.COMMENTS)
                .doc(comment.parentId);


            const parentCommentSnap = await parentCommentRef.get();
            
            // parent comment might not exist. This case can happen 
            // during mass deletion of comments in a channel [onCommentChannelDeleted]
            if (parentCommentSnap.exists) {
                const changes = {
                    repliesCount: FieldValue.increment(-1),
                    updatedAt: now
                };    
    
                parentCommentRef.update(changes);
            }
        }


        // check if it's last comment
        const parentChannelRef = admin.firestore().collection(Constants.COMMENT_CHANNELS).doc(comment.commentChannelId);

        // we need to be careful here because, the parent channel might not be there anymore
        const parentChannelSnap = await parentChannelRef.get();
        
        // if parent channel exists only then can we continue further 
        // [Parent channel might be deleted in the case of mass deletion of comments 
        // @see onCommentChannelDeleted]
        if (!parentChannelSnap.exists) {
            console.log("Parent channel doesn't exist");
            
            return {
                response: "Parent channel doesn't exist anymore."
            };
        }


        const parentChannel = convertSnapshotToCommentChannel(parentChannelSnap);

        console.log("Parent channel does exist : " + parentChannel.commentChannelId);

        const lastComment = parentChannel.lastComment;
        // checking if this was the last comment
        if (lastComment && lastComment.commentId === comment.commentId) {
            console.log("Last comment is not null and it's the same as the current comment " + lastComment.commentId);
            
            // if it was, check if it was the only comment
            if (parentChannel.commentsCount == 1) { // remember that parent channel is not informed of this comment being deleted
                // only comment

                console.log("Parent channel contains only this current comment");

                const parentChannelChanges = {
                    updatedAt: now,
                    commentsCount: FieldValue.increment(-1), // the comment that just got deleted
                    lastComment: null
                };

                return parentChannelRef.update(parentChannelChanges);
            } else {
                console.log("Parent channel contains more comments than this current comment");

                // get the second last comment
                const secondLastCommentCollection = await parentChannelRef.collection(Constants.COMMENTS)
                    .orderBy(Constants.CREATED_AT, "desc")
                    .startAfter(comment.createdAt)
                    .limit(1)
                    .get();


                // if it is not 1, it is an unexpected error, because we have checked that there are more than 1 comment    
                if (secondLastCommentCollection.size == 1) {
                    console.log("Second last comment collection size is 1");    

                    const secondLastCommentSnap = secondLastCommentCollection.docs.at(0);
                    if (secondLastCommentSnap) {
                        console.log("Second last comment snap is not null"); 

                        const secondLastComment = convertSnapshotToComment(secondLastCommentSnap);
                        
                        const parentChannelChanges = {
                            updatedAt: now,
                            commentsCount: FieldValue.increment(-1), // the comment that just got deleted
                            lastComment: secondLastComment
                        };
        
                        return parentChannelRef.update(parentChannelChanges);
                    } else {
                        console.log("Second last comment snap is null");  

                        return {
                            reponse: "Second last comment snapshot is undefined."
                        };
                    }
                } else {
                    console.log("Second last comment collection size is not 1");  

                    return {
                        response: "We have limited it to 1, but there were no documents," + 
                        "which mean the size was only including the last comment, which" + 
                        "is not possible at this stage of code because we have checked it with if condition."
                    };
                }
            }
        } else {
            console.log("Last comment is either null or it's not the same as the current comment " + lastComment);

            // if it is anything otherwise, we just have to update the parent channel
            const parentChannelChanges = {
                updatedAt: now,
                commentsCount: FieldValue.increment(-1), // the comment that just got deleted
            };

            return parentChannelRef.update(parentChannelChanges);
        }
    } catch (error) {
        return {
            response: "OnCommentDelete: TransactionError"
        };
    }       
});

/**
 * Cleanup process after a comment is deleted, deleting all child nodes
 */
export const onCommentChannelDeleted = functions.firestore.document("commentChannels/{commentChannelId}")
    .onDelete(async (snap, context) => {
        const documents = await snap.ref.collection(Constants.COMMENTS).listDocuments();

        if (documents.length == 0) {
            return {
                response: `There are no comments in this thread channel. [${context.params.commentChannelId}]`
            };
        }

        const batch = admin.firestore().batch();
        documents.forEach((doc, _i) => {
            batch.delete(doc);
        });

        return await batch.commit();
    });


/**
 * 
 * @param {string|string[]} tokens The registration tokens of the users to whom the notification is to be sent
 * @param {Record<string, string>} data The notification payload that contains extra notification data
 * @return {Promise<MessagingDeviceResponse>}
 */
export const sendNotification = async (tokens: string | string[], data: Record<string, string>): Promise<MessagingDevicesResponse> => {
    const payload = {
        notification: {
            title: data.title,
            body: data.content,
            sound: "default"
        },
        data: data
    };

    return await admin.messaging().sendToDevice(tokens, payload, { priority: "high" });
};


/**
 * 
 * @param {string} topic The topic to which the notification is to be sent
 * @param {NotificationPayload} data The notification payload that contains extra notification data
 * @return {Promise<MessageTopicResponse>|any}
 */
export const sendNotificationToTopic = async (topic: string, data: NotificationPayload) => {
    let payLoad = {};

    if (data.image != undefined) {
        payLoad = {
            notification: {
                title: data.title,
                body: data.content,
                image: data.image,
                imageUrl: data.image,
                sound: data.sound
            },
            data: data
        };
    } else {
        payLoad = {
            notification: {
                title: data.title,
                body: data.content,
                sound: data.sound
            },
            data: data
        };
    }

    try {
        return await admin.messaging().sendToTopic(topic, payLoad, { priority: "high" });
    } catch (error) {
        return {
            response: `Error sending message:, ${error}`
        };
    }
};


/**
 * A utility function to verify subscription purchased by users
 */
export const verifyPurchase = functions.https.onCall(async (data, _context) => {
    const purchaseInfo = getPurchaseInfoFromObject(data);

    const userId = data.userId;

    const docSnap = await admin.firestore()
        .collection(Constants.USERS)
        .doc(userId)
        .collection(Constants.PURCHASES)
        .doc(purchaseInfo.purchaseToken)
        .get();

    if (docSnap.exists) {
        return purchaseInfo;
    } else {
        purchaseInfo.isValid = true;

        await admin.firestore()
            .collection(Constants.USERS)
            .doc(userId)
            .collection(Constants.PURCHASES)
            .doc(purchaseInfo.purchaseToken)
            .set(purchaseInfo);

        return purchaseInfo;
    }
});


// export const createVideoThumb = functions.storage.object()
//     .onFinalize(async (object, context) => {
//         const fileBucket = object.bucket;
//         const filePath = object.name;
//         const contentType = object.contentType;


//         if (filePath && contentType) {
//             const dir = path.dirname(filePath);
//             const fileName = path.basename(filePath);

//             const name = fileName.split(".")[0];

//             functions.logger.log(fileBucket, filePath, contentType, dir, fileName);

//             if (!contentType.startsWith("video/")) {
//                 return {
//                     response: "Not a video"
//                 };
//             }

//             if (fileName.startsWith("thumb_")) {
//                 return {
//                     response: "Video is already a thumbnail"
//                 };
//             }
                 
//             const token = uuid();
//             const bucket = admin.storage().bucket(fileBucket);
//             const tempFilePath = path.join(os.tmpdir(), fileName);

//             try {
//                 const upRes = await bucket.file(filePath)
//                     .download({ destination: tempFilePath });
                
//                 console.log("UP_RES", JSON.stringify(upRes));
//                 functions.logger.log("Video has been download to: ", tempFilePath);    
//             } catch (error) {
//                 console.log("Error at download bucket", error);
//             }

//             const thumbFileName = `thumb_${name}.jpg`;
//             const locThumbFilePath = path.join(os.tmpdir(), thumbFileName);
//             const remoteThumbFilePath = path.join(dir, thumbFileName);

//             await spawn.spawn("ffmpeg", ["-i", tempFilePath, "-vframes", "1", "-an", "-s", "400X300", "-ss", "10", locThumbFilePath]);
        
//             functions.logger.log("Thumbnail has been created");

//             await bucket.upload(locThumbFilePath, {
//                 destination: remoteThumbFilePath,
//                 metadata: {
//                     contentType: "image/jpg",
//                     metadata: {
//                         firebaseStorageDownloadTokens: token
//                     }
//                 },
//                 public: true,
//             });

//             functions.logger.log("Thumbnail uploaded to the bucket");

//             fs.unlinkSync(locThumbFilePath);
//             return fs.unlinkSync(tempFilePath);
//         }
//     });
