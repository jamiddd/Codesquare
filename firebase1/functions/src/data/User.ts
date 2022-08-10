import { MyLocation } from "./MyLocation";

export interface User {
    id: string;
    name: string;
    username: string;
    tag: string;
    email: string;
    about: string;
    photo: string;
    token: string;
    postsCount: number;
    collaborationsCount: number;
    likesCount: number;
    likedPostsCount: number;
    likedUsersCount: number;
    likedCommentsCount: number;
    savedPostsCount: number;
    upvotedPostsCount: number;
    downvotedPostsCount: number;
    createdAt: number;
    updatedAt: number;
    premiumState: number;
    online: boolean;
    interests: string[];
    archivedProjects: string[];
    collaborations: string[];
    posts: string[];
    postRequests: string[];
    postInvites: string[];
    chatChannels: string[];
    blockedUsers: string[];
    blockedBy: string[];
    location: MyLocation;
}
