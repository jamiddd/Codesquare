import { MyLocation } from "./MyLocation";

export interface User {
    id: string;
    name: string;
    username: string;
    tag: string;
    email: string;
    about: string;
    photo: string;
    interests: string[];
    likedUsers: string[];
    likedProjects: string[];
    likedComments: string[];
    savedProjects: string[];
    archivedProjects: string[];
    collaborations: string[];
    posts: string[];
    postRequests: string[];
    postInvites: string[];
    chatChannels: string[];
    token: string;
    postsCount: number;
    collaborationsCount: number;
    likesCount: number;
    createdAt: number;
    updatedAt: number;
    location: MyLocation;
    premiumState: number;
    online: boolean
}
