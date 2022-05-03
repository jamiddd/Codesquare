import { MyLocation } from "./MyLocation";
import { UserMinimal } from "./UserMinimal";

/**
 * Implementaion class of Project interface
 * 
 * Project is post in CollabMe app
 */
export class PostImpl {
    /**
     * 
     * @param {string} id Id of the post
     * @param {string} name Name of the post
     * @param {string} content Content of the post
     * @param {string} commentChannel Comment channel associated with the post
     * @param {string} chatChannel Chat channel associated with the post
     * @param {UserMinimal} creator Creator of the post
     * @param {number} likesCount Count of number of likes for this post
     * @param {number} commentsCount Count of number of comments in this post
     * @param {string[]} contributors Number of people contributing to this post 
     * @param {string[]} images List of images of this post
     * @param {string[]} tags List of tags associated with this post
     * @param {string[]} sources List of sources associated with this post
     * @param {string[]} requests List of requests sent by other users
     * @param {MyLocation} location Location of this post
     * @param {number} createdAt Time of creation of this post
     * @param {number} updatedAt Time of updation of this post
     * @param {number} expiredAt Time of expiry of this post
     * @param {number} viewsCount Count of number of views of this post
     * @param {boolean} archived State of the project of being archived
     * @param {string[]} blockedList List of users who are blocked for this post
     */
    constructor(
        readonly id: string,
        readonly name: string,
        readonly content: string,
        readonly commentChannel: string,
        readonly chatChannel: string,
        readonly creator: UserMinimal,
        readonly likesCount: number,
        readonly commentsCount: number,
        readonly contributors: string[],
        readonly images: string[],
        readonly tags: string[],
        readonly sources: string[],
        readonly requests: string[],
        readonly location: MyLocation,
        readonly createdAt: number,
        readonly updatedAt: number,
        readonly expiredAt: number,
        readonly viewsCount: number,
        readonly archived: boolean,
        readonly blockedList: string[]
        ) {}
}
