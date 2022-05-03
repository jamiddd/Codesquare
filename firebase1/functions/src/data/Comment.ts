import { UserMinimal } from "./UserMinimal";

export interface Comment {
    commentId: string;
    content: string;
    senderId: string;
    sender: UserMinimal;
    parentId: string; // can be a post or comment
    postId: string;
    commentChannelId: string;
    threadChannelId: string;
    likesCount: number;
    repliesCount: number;
    commentLevel: number;
    createdAt: number;
    updatedAt: number;
    parentCommentChannelId?: string;
}
