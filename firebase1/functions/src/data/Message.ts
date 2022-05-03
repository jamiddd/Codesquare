import { MessageMinimal } from "./MessageMinimal";
import { Metadata } from "./Metadata";
import { UserMinimal } from "./UserMinimal";

export interface Message {
    messageId: string;
    chatChannelId: string;
    type: string;
    content: string;
    senderId: string;
    sender: UserMinimal;
    metadata?: Metadata;
    deliveryList: string[];
    readList: string[];
    createdAt: number;
    updatedAt: number;
    replyTo?: string;
    replyMessage?: MessageMinimal;
}
