import { UserMinimal } from "./UserMinimal";

export interface MyNotification {
    id: string;
    title: string;
    content: string;
    senderId: string;
    receiverId: string;
    sender: UserMinimal;
    createdAt: number;
    updatedAt: number;
    type: number;
    read: boolean;
    image?: string;
    postId?: string;
    commentChannelId?: string;
    commentId?: string;
    userId?: string;
}
