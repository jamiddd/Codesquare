import { Message } from "./Message";
import { UserMinimal } from "./UserMinimal";

export interface ChatChannel {
    chatChannelId: string;
    postId: string;
    postTitle: string;
    postImage: string;
    type: string;
    rules: string;
    administrators: string[];
    contributors: string[];
    tokens: string[];
    blockedUsers: string[];
    contributorsCount: number;
    createdAt: number;
    updatedAt: number;
    mute: boolean;
    archived: boolean;
    authorized: boolean;
    lastMessage?: Message;
    data1?: UserMinimal;
    data2?: UserMinimal;
}
