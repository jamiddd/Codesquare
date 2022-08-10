import { MyLocation } from "./MyLocation";

/**
 * Implementation of user interface
 * 
 * A user is an account data of a person using the app 
 */
export class UserImpl {
    /**
     * 
     * @param {string} id id
     * @param {string} name name
     * @param {string} username username
     * @param {string} tag tag
     * @param {string} email email 
     * @param {string} about about 
     * @param {string} photo photo 
     * @param {string} token token 
     * @param {number} postsCount postsCount 
     * @param {number} collaborationsCount collaborationsCount 
     * @param {number} likesCount likesCount 
     * @param {number} likedPostsCount likedPostsCount 
     * @param {number} likedUsersCount likedUsersCount 
     * @param {number} likedCommentsCount likedCommentsCount 
     * @param {number} savedPostsCount savedPostsCount 
     * @param {number} upvotedPostsCount upvotedPostsCount
     * @param {number} downvotedPostsCount downvotedPostsCount 
     * @param {number} createdAt createdAt 
     * @param {number} updatedAt updatedAt 
     * @param {number} premiumState premiumState 
     * @param {boolean} online online 
     * @param {string[]} interests interests 
     * @param {string[]} archivedPosts archivedPosts 
     * @param {string[]} collaborations collaborations 
     * @param {string[]} posts posts 
     * @param {string[]} postRequests postRequests 
     * @param {string[]} postInvites postInvites 
     * @param {string[]} chatChannels chatChannels 
     * @param {string[]} blockedUsers blockedUsers 
     * @param {string[]} blockedBy blockedBy 
     * @param {MyLocation} location location 
     */
    constructor(
        readonly id: string,
        readonly name: string,
        readonly username: string,
        readonly tag: string,
        readonly email: string,
        readonly about: string,
        readonly photo: string,
        readonly token: string,
        readonly postsCount: number,
        readonly collaborationsCount: number,
        readonly likesCount: number,
        readonly likedPostsCount: number,
        readonly likedUsersCount: number,
        readonly likedCommentsCount: number,
        readonly savedPostsCount: number,
        readonly upvotedPostsCount: number,
        readonly downvotedPostsCount: number,
        readonly createdAt: number,
        readonly updatedAt: number,
        readonly premiumState: number,
        readonly online: boolean,
        readonly interests: string[],
        readonly archivedPosts: string[],
        readonly collaborations: string[],
        readonly posts: string[],
        readonly postRequests: string[],
        readonly postInvites: string[],
        readonly chatChannels: string[],
        readonly blockedUsers: string[],
        readonly blockedBy: string[],
        readonly location: MyLocation,
    ) {

    }
}
