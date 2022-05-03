import { MyLocation } from "./MyLocation";
import { UserMinimal } from "./UserMinimal";

export interface Post {
    id: string;
    name: string;
    content: string;
    commentChannel: string;
    chatChannel: string;
    creator: UserMinimal;
    likesCount: number;
    commentsCount: number;
    contributors: string[];
    images: string[];
    tags: string[];
    sources: string[];
    requests: string[];
    location: MyLocation;
    createdAt: number;
    updatedAt: number;
    expiredAt: number;
    viewsCount: number;
    archived: boolean;
    blockedList: string[];
}
