import { UserMinimal } from "./UserMinimal";
import { MyLocation } from "./MyLocation";

export interface PostMinimal {
    objectID: string;
    type: string;
    name: string;
    content: string;
    createdAt: number;
    creator: UserMinimal;
    images: string[];
    location: MyLocation;
    tags: string[];
    updatedAt: number;
}
