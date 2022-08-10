import { UserMinimal } from "./UserMinimal";
import { MyLocation } from "./MyLocation";

export interface PostMinimal {
    objectID: string;
    type: string;
    name: string;
    content: string;
    thumbnail: string;
    chatChannel: string;
    createdAt: number;
    updatedAt: number;
    rank: number;
    mediaList: string[];
    tags: string[];
    creator: UserMinimal;
    location: MyLocation;
}
