import { Metadata } from "./Metadata";

export interface MessageMinimal {
    senderId: string;
    name: string;
    content: string;
    type: string;
    messageId: string;
    chatChannelId: string;
    metadata?: Metadata;
}
