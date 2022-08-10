export interface NotificationPayload {
    title: string;
    content: string;
    senderId: string;
    receiverId: string;
    notificationId: string;
    type: string;
    image?: string;
    sound?: string;
    postId?: string;
    commentChannelId?: string;
    userId?: string;
    commentId?: string;
}
