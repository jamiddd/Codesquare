import * as Constants from "./Constants";
import { MyNotification } from "../data/MyNotification";
import { UserMinimal } from "../data/UserMinimal";
import { PostMinimal } from "../data/PostMinimal";
import { MyLocation } from "../data/MyLocation";
import { MyTag } from "../data/MyTag";
import { UserMinimal2 } from "../data/UserMinimal2";
import { ChatChannel } from "../data/ChatChannel";
import { Post } from "../data/Post";
import { PurchaseInfo } from "../data/PurchaseInfo";
import { DocumentSnapshot, QueryDocumentSnapshot, DocumentData } from "firebase-admin/firestore";
import { PostImpl } from "../data/PostImpl";
import { ChatChannelImpl } from "../data/ChatChannelImpl";
import { UserImpl } from "../data/UserImpl";
import { UserMinimal2Impl } from "../data/UserMinimal2Impl";
import { PostMinimalImpl } from "../data/PostMinimalImpl";
import { Message } from "../data/Message";
import { Metadata } from "../data/Metadata";
import { MessageMinimal } from "../data/MessageMinimal";
import { Comment } from "../data/Comment";
import { CommentChannel } from "../data/CommentChannel";
import { InterestItem } from "../data/InterestItem";


export const getTagObjectsFromArray = (tags: string[]): MyTag[] => {
    const tagObjects: MyTag[] = [];

    if (tags.length == 0) {
        return tagObjects;
    }

    for (let i = 0; i < tags.length; i++) {
        const s = tags[i];
        const id = s.toLowerCase().split(" ").join("_");

        const myTag = {
            objectID: id,
            interest: s
        };

        tagObjects.push(myTag);
    }

    return tagObjects;
};

export const getPurchaseInfoFromObject = (data: any): PurchaseInfo => {
    const purchaseInfo: PurchaseInfo = {
        purchaseToken: data.purchaseToken,
        orderId: data.purchaseOrderId,
        purchaseTime: data.purchaseTime,
        productId: data.productId,
        isValid: false
    };
    return purchaseInfo;
};

export const convertSnapshotToPost = (snapshot: QueryDocumentSnapshot|DocumentSnapshot): Post => {
    const id = snapshot.get(Constants.ID);
    const name = snapshot.get(Constants.NAME);
    const content = snapshot.get(Constants.CONTENT);
    const commentChannel = snapshot.get(Constants.COMMENT_CHANNEL);
    const chatChannel = snapshot.get(Constants.CHAT_CHANNEL);
    const creator = snapshot.get(Constants.CREATOR);
    const likesCount = snapshot.get(Constants.LIKES_COUNT);
    const commentsCount = snapshot.get(Constants.COMMENTS_COUNT);
    const contributors = snapshot.get(Constants.CONTRIBUTORS);
    const images = snapshot.get(Constants.IMAGES);
    const tags = snapshot.get(Constants.TAGS);
    const sources = snapshot.get(Constants.SOURCES);
    const requests = snapshot.get(Constants.REQUESTS);
    const location = snapshot.get(Constants.LOCATION);
    const createdAt = snapshot.get(Constants.CREATED_AT);
    const updatedAt = snapshot.get(Constants.UPDATED_AT);
    const expiredAt = snapshot.get(Constants.EXPIRED_AT);
    const viewsCount = snapshot.get(Constants.VIEWS_COUNT);
    const archived = snapshot.get(Constants.ARCHIVED);
    const blockedList = snapshot.get(Constants.BLOCKED_LIST);

    const post: Post = {
        id,
        name,
        content,
        commentChannel,
        chatChannel,
        creator,
        likesCount,
        commentsCount,
        contributors,
        images,
        tags,
        sources,
        requests,
        location,
        createdAt,
        updatedAt,
        expiredAt,
        viewsCount,
        archived,
        blockedList
    };

    return post;
};

export const convertSnapshotToChatChannel = (snapshot: QueryDocumentSnapshot|DocumentSnapshot): ChatChannel => {
    const chatChannelId = snapshot.get(Constants.CHAT_CHANNEL_ID);
    const postId = snapshot.get(Constants.POST_ID);
    const postTitle = snapshot.get(Constants.POST_TITLE);
    const postImage = snapshot.get(Constants.POST_IMAGE);
    const contributorsCount = snapshot.get(Constants.CONTRIBUTORS_COUNT);
    const administrators = snapshot.get(Constants.ADMINISTRATORS);
    const contributors = snapshot.get(Constants.CONTRIBUTORS);
    const rules = snapshot.get(Constants.RULES);
    const createdAt = snapshot.get(Constants.CREATED_AT);
    const updatedAt = snapshot.get(Constants.UPDATED_AT);
    const lastMessage = snapshot.get(Constants.LAST_MESSAGE);
    const tokens = snapshot.get(Constants.TOKENS);
    const blockedUsers = snapshot.get(Constants.BLOCKED_USERS);
    const archived = snapshot.get(Constants.ARCHIVED);

    const chatChannel: ChatChannel = {
        chatChannelId,
        postId,
        postTitle,
        postImage,
        contributorsCount,
        administrators,
        contributors,
        rules,
        createdAt,
        updatedAt,
        lastMessage,
        tokens,
        blockedUsers,
        archived
    };

    return chatChannel;
};

export const chatChannelConverter: FirebaseFirestore.FirestoreDataConverter<ChatChannelImpl> = {
    toFirestore: function(chatChannel: ChatChannelImpl): DocumentData {
        return {
            chatChannelId: chatChannel.chatChannelId,
            postId: chatChannel.postId,
            postTitle: chatChannel.postTitle,
            postImage: chatChannel.postImage,
            contributorsCount: chatChannel.contributorsCount,
            administrators: chatChannel.administrators,
            contributors: chatChannel.contributors,
            rules: chatChannel.rules,
            createdAt: chatChannel.createdAt,
            updatedAt: chatChannel.updatedAt,
            tokens: chatChannel.tokens,
            blockedUsers: chatChannel.blockedUsers,
            archived: chatChannel.archived,
            lastMessage: chatChannel.lastMessage,
        };
    },
    fromFirestore: function(snapshot: QueryDocumentSnapshot<DocumentData>): ChatChannelImpl {
        const data = snapshot.data();
        return new ChatChannelImpl(
            data.chatChannelId,
            data.postId,
            data.postTitle,
            data.postImage,
            data.contributorsCount,
            data.administrators,
            data.contributors,
            data.rules,
            data.createdAt,
            data.updatedAt,
            data.tokens,
            data.blockedUsers,
            data.archived,
            data.lastMessage
        );
    }
};

export const postConverter: FirebaseFirestore.FirestoreDataConverter<PostImpl> = {
    toFirestore: function(post: PostImpl): DocumentData {
        return {
            id: post.id,
            name: post.name,
            content: post.content,
            commentChannel: post.commentChannel,
            chatChannel: post.chatChannel,
            creator: post.creator,
            likes: post.likesCount,
            comments: post.commentsCount,
            contributors: post.contributors,
            images: post.images,
            tags: post.tags,
            sources: post.sources,
            requests: post.requests,
            location: post.location,
            createdAt: post.createdAt,
            updatedAt: post.updatedAt,
            expiredAt: post.expiredAt,
            viewsCount: post.viewsCount,
            archived: post.archived,
            blockedList: post.blockedList
        };
    },
    fromFirestore: function(snapshot: QueryDocumentSnapshot<DocumentData>): PostImpl {
        const data = snapshot.data();
        return new PostImpl(
            data.id,
            data.name,
            data.content,
            data.commentChannel,
            data.chatChannel,
            data.creator,
            data.likesCount,
            data.commentsCount,
            data.contributors,
            data.images,
            data.tags,
            data.sources,
            data.requests,
            data.location,
            data.createdAt,
            data.updatedAt,
            data.expiredAt,
            data.viewsCount,
            data.archived,
            data.blockedList
        );
    }
};

export const userConverter: FirebaseFirestore.FirestoreDataConverter<UserImpl> = {
    toFirestore: function(user: UserImpl): DocumentData {
        return {
            id: user.id,
            name: user.name,
            username: user.username,
            tag: user.tag,
            email: user.email,
            about: user.about,
            photo: user.photo,
            interests: user.interests,
            savedPosts: user.savedPosts,
            archivedPosts: user.archivedPosts,
            collaborations: user.collaborations,
            posts: user.posts,
            postRequests: user.postRequests,
            postInvites: user.postInvites,
            chatChannels: user.chatChannels,
            token: user.token,
            postsCount: user.postsCount,
            collaborationsCount: user.collaborationsCount,
            likesCount: user.likesCount,
            createdAt: user.createdAt,
            updatedAt: user.updatedAt,
            location: user.location,
            premiumState: user.premiumState,
            online: user.online
        };
    },
    fromFirestore: function(snapshot: QueryDocumentSnapshot<DocumentData>): UserImpl {
        const data = snapshot.data();
        return new UserImpl(
            data.id,
            data.name,
            data.username,
            data.tag,
            data.email,
            data.about,
            data.photo,
            data.interests,
            data.savedPosts,
            data.archivedPosts,
            data.collaborations,
            data.posts,
            data.postRequests,
            data.postInvites,
            data.chatChannels,
            data.token,
            data.postsCount,
            data.collaborationsCount,
            data.likesCount,
            data.createdAt,
            data.updatedAt,
            data.location,
            data.premiumState,
            data.online
        );
    }
};

export const userMinimal2Converter: FirebaseFirestore.FirestoreDataConverter<UserMinimal2Impl> = {
    toFirestore: function(userMinimal: UserMinimal2Impl): DocumentData {
        return {
            objectID: userMinimal.objectID,
            email: userMinimal.email,
            about: userMinimal.about,
            createdAt: userMinimal.createdAt,
            interests: userMinimal.interests,
            location: userMinimal.location,
            name: userMinimal.name,
            premiumState: userMinimal.premiumState,
            tag: userMinimal.tag,
            username: userMinimal.username,
            type: userMinimal.type,
            photo: userMinimal.photo
        };
    },
    fromFirestore: function(snapshot: QueryDocumentSnapshot<DocumentData>): UserMinimal2Impl {
        const data = snapshot.data();
        return new UserMinimal2Impl(
            data.objectID,
            data.email,
            data.about,
            data.createdAt,
            data.interests,
            data.location,
            data.name,
            data.premiumState,
            data.tag,
            data.username,
            data.type,
            data.photo
        );
    }
};

export const postMinimalConverter: FirebaseFirestore.FirestoreDataConverter<PostMinimalImpl> = {
    toFirestore: function(postMinimal: PostMinimalImpl): DocumentData {
        return {
            objectID: postMinimal.objectID,
            type: postMinimal.type,
            name: postMinimal.name,
            content: postMinimal.content,
            createdAt: postMinimal.createdAt,
            creator: postMinimal.creator,
            images: postMinimal.images,
            location: postMinimal.location,
            tags: postMinimal.tags,
            updatedAt: postMinimal.updatedAt
        };
    },
    fromFirestore: function(snapshot: QueryDocumentSnapshot<DocumentData>): PostMinimalImpl {
        const data = snapshot.data();
        return new PostMinimalImpl(
            data.objectID,
            data.type,
            data.name,
            data.content,
            data.createdAt,
            data.creator,
            data.images,
            data.location,
            data.tags,
            data.updatedAt
        );
    }
};


export const convertSnapshotToInterestItem = (snap: QueryDocumentSnapshot|DocumentSnapshot): InterestItem => {
    const itemId: string = snap.get("itemId");
    const objectID: string = snap.get(Constants.OBJECT_ID);
    const content: string = snap.get(Constants.CONTENT);
    const createdAt: number = snap.get(Constants.CREATED_AT);
    const associations: string[] = snap.get("associations");
    const weight: number = snap.get("weight");
    const updatedAt: number = snap.get(Constants.UPDATED_AT);

    const interestItem: InterestItem = {
        itemId,
        objectID,
        content,
        createdAt,
        associations,
        weight,
        updatedAt
    };

    return interestItem;
};

export const convertSnapshotToComment = (snap: QueryDocumentSnapshot|DocumentSnapshot): Comment => {
    const commentId: string = snap.get(Constants.COMMENT_ID);
    const content: string = snap.get(Constants.CONTENT);
    const senderId: string = snap.get(Constants.SENDER_ID);
    const sender: UserMinimal = snap.get(Constants.SENDER);
    const parentId: string = snap.get(Constants.PARENT_ID);
    const postId: string = snap.get(Constants.POST_ID);
    const commentChannelId: string = snap.get(Constants.COMMENT_CHANNEL_ID);
    const threadChannelId: string = snap.get(Constants.THREAD_CHANNEL_ID);
    const likesCount: number = snap.get(Constants.LIKES_COUNT);
    const repliesCount: number = snap.get(Constants.REPLIES_COUNT);
    const commentLevel: number = snap.get(Constants.COMMENT_LEVEL);
    const createdAt: number = snap.get(Constants.CREATED_AT);
    const updatedAt: number = snap.get(Constants.UPDATED_AT);
    const parentCommentChannelId: string|undefined = snap.get(Constants.PARENT_COMMENT_CHANNEL_ID);

    const comment: Comment = {
        commentId,
        content,
        senderId,
        sender,
        parentId,
        postId,
        commentChannelId,
        threadChannelId,
        likesCount,
        repliesCount,
        commentLevel,
        createdAt,
        updatedAt,
        parentCommentChannelId
    };

    return comment;
};

export const convertSnapshotToCommentChannel = (snap: QueryDocumentSnapshot|DocumentSnapshot): CommentChannel => {
    const commentChannelId: string = snap.get(Constants.COMMENT_CHANNEL_ID);
    const parentId: string = snap.get(Constants.PARENT_ID);
    const postId: string = snap.get(Constants.POST_ID);
    const postTitle: string = snap.get(Constants.POST_TITLE);
    const commentsCount: number = snap.get(Constants.COMMENTS_COUNT);
    const createdAt: number = snap.get(Constants.CREATED_AT);
    const archived: boolean = snap.get(Constants.ARCHIVED);
    const updatedAt: number = snap.get(Constants.UPDATED_AT);
    const lastComment: Comment|undefined = snap.get(Constants.LAST_COMMENT);
    
    const commentChannel: CommentChannel = {
        commentChannelId,
        parentId,
        postId,
        postTitle,
        commentsCount,
        createdAt,
        archived,
        updatedAt,
        lastComment
    };

    return commentChannel;
};

export const convertSnapshotToUserMinimal = (snapshot: QueryDocumentSnapshot): UserMinimal2 => {
    const objectID: string = snapshot.get(Constants.ID);
    const type: string = Constants.USER;
    const name: string = snapshot.get(Constants.NAME);
    const email: string = snapshot.get(Constants.EMAIL);
    const about: string = snapshot.get(Constants.ABOUT);
    const createdAt: number = snapshot.get(Constants.CREATED_AT);
    const interests: string[] = snapshot.get(Constants.INTERESTS);
    const location: MyLocation = snapshot.get(Constants.LOCATION);
    const photo: string|undefined = snapshot.get(Constants.PHOTO);
    const premiumState: number = snapshot.get(Constants.PREMIUM_STATE);
    const tag: string = snapshot.get(Constants.TAG);
    const username: string = snapshot.get(Constants.USERNAME);
    

    const userMinimal: UserMinimal2 = {
        objectID,
        email,
        about,
        createdAt,
        interests,
        location,
        name,
        photo,
        premiumState,
        tag,
        username,
        type
    };

    return userMinimal;
};

export const convertSnapshotToMessage = (snapshot: QueryDocumentSnapshot|DocumentSnapshot): Message => {
    const messageId: string = snapshot.get(Constants.MESSAGE_ID);
    const chatChannelId: string = snapshot.get(Constants.CHAT_CHANNEL_ID);
    const type: string = snapshot.get(Constants.TYPE);
    const content: string = snapshot.get(Constants.CONTENT);
    const senderId: string = snapshot.get(Constants.SENDER_ID);
    const sender: UserMinimal = snapshot.get(Constants.SENDER);
    const metadata: Metadata = snapshot.get(Constants.METADATA);
    const deliveryList: string[] = snapshot.get(Constants.DELIVERY_LIST);
    const readList: string[] = snapshot.get(Constants.READ_LIST);
    const createdAt: number = snapshot.get(Constants.CREATED_AT);
    const updatedAt: number = snapshot.get(Constants.UPDATED_AT);
    const replyTo: string|undefined = snapshot.get(Constants.REPLY_TO);
    const replyMessage: MessageMinimal|undefined = snapshot.get(Constants.REPLY_MESSAGE);

    const message: Message = {
        messageId,
        chatChannelId,
        type,
        content,
        senderId,
        sender,
        metadata,
        deliveryList,
        readList,
        createdAt,
        updatedAt,
        replyTo,
        replyMessage
    };

    return message;
};

export const convertSnapshotToPostMinimal = (snapshot: QueryDocumentSnapshot): PostMinimal => {
    const objectID: string = snapshot.get(Constants.ID);
    
    // type is not found in post document
    const type: string = Constants.POST;
    const name: string = snapshot.get(Constants.NAME);
    const content: string = snapshot.get(Constants.CONTENT);
    const createdAt: number = snapshot.get(Constants.CREATED_AT);
    const creator: UserMinimal = snapshot.get(Constants.CREATOR);
    const images: string[] = snapshot.get(Constants.IMAGES);
    const location: MyLocation = snapshot.get(Constants.LOCATION);
    const tags: string[] = snapshot.get(Constants.TAGS);
    const updatedAt: number = snapshot.get(Constants.UPDATED_AT);

    const postMinimal: PostMinimal = {
        objectID,
        type,
        name,
        content,
        createdAt,
        creator,
        images,
        location,
        tags,
        updatedAt
    };

    return postMinimal;
};

export const convertSnapshotToNotification = (snapshot: QueryDocumentSnapshot): MyNotification => {
    const id: string = snapshot.get(Constants.ID);
    const title: string = snapshot.get(Constants.TITLE);
    const content: string = snapshot.get(Constants.CONTENT);
    const senderId: string = snapshot.get(Constants.SENDER_ID);
    const receiverId: string = snapshot.get(Constants.RECEIVER_ID);
    const sender: UserMinimal = snapshot.get(Constants.SENDER_ID);
    const createdAt: number = snapshot.get(Constants.CREATED_AT);
    const updatedAt: number = snapshot.get(Constants.UPDATED_AT);
    const type: number = snapshot.get(Constants.TYPE);
    const read: boolean = snapshot.get(Constants.READ);
    const image: string|undefined = snapshot.get(Constants.IMAGE);
    const postId: string|undefined = snapshot.get(Constants.POST_ID);
    const commentChannelId: string|undefined = snapshot.get(Constants.COMMENT_CHANNEL_ID);
    const commentId: string|undefined = snapshot.get(Constants.COMMENT_ID);
    const userId: string|undefined = snapshot.get(Constants.USER_ID);

    const notification: MyNotification = {
        id,
        title,
        content,
        senderId,
        receiverId,
        sender,
        createdAt,
        updatedAt,
        type,
        read,
        image,
        postId,
        commentChannelId,
        commentId,
        userId
    };

    return notification;
};
