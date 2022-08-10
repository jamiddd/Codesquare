import { Comment } from "./Comment";

export interface CommentChannel {
    commentChannelId: string;
    parentId: string; // can be a comment or a post
    postId: string;
    commentsCount: number;
    createdAt: number;
    archived: boolean;
    updatedAt: number;
    lastComment?: Comment;
}
