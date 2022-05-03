import { Message } from "./Message";

export interface ChatChannel {
    chatChannelId: string;
    postId: string;
    postTitle: string;
    postImage: string;
    contributorsCount: number;
    administrators: string[];
    contributors: string[];
    rules: string;
    createdAt: number;
    updatedAt: number;
    lastMessage?: Message;
    tokens: string[];
    blockedUsers: string[];
    archived: boolean
}
