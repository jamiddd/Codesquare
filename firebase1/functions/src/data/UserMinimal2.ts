import { MyLocation } from "./MyLocation";

export interface UserMinimal2 {
    objectID: string;
    email: string;
    about: string;
    createdAt: number;
    interests: string[];
    location: MyLocation;
    name: string;
    photo?: string;
    premiumState: number;
    tag: string;
    username: string;
    type: string
}
