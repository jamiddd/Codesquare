import { MyLocation } from "./MyLocation";
import { UserMinimal } from "./UserMinimal";

export interface Post {
    id: string;
    name: string;
    content: string;
    commentChannel: string;
    chatChannel: string;
    rankCategory: string;
    thumbnail: string;
    mediaString: string;
    likesCount: number;
    commentsCount: number;
    contributorsCount: number;
    createdAt: number;
    updatedAt: number;
    expiredAt: number;
    viewsCount: number;
    rank: number;
    points: number;
    blockedList: string[];
    mediaList: string[];
    tags: string[];
    sources: string[];
    contributors: string[];
    requests: string[];
    archived: boolean;
    creator: UserMinimal;
    location: MyLocation;
}
