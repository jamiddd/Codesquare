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
     * @param {string} id {string} id 
     * @param {string} name  name 
     * @param {string} content content 
     * @param {string} commentChannel commentChannel 
     * @param {string} chatChannel chatChannel 
     * @param {string} rankCategory rankCategory 
     * @param {string} thumbnail thumbnail 
     * @param {string} mediaString mediaString 
     * @param {number} likesCount likesCount 
     * @param {number} commentsCount commentsCount 
     * @param {number} contributorsCount contributorsCount 
     * @param {number} createdAt createdAt 
     * @param {number} updatedAt updatedAt 
     * @param {number} expiredAt expiredAt 
     * @param {number} viewsCount viewsCount 
     * @param {number} rank rank 
     * @param {number} points points 
     * @param {string[]} blockedList blockedList 
     * @param {string[]} mediaList mediaList 
     * @param {string[]} tags tags 
     * @param {string[]} sources sources 
     * @param {string[]} contributors contributors 
     * @param {string[]} requests requests 
     * @param {boolean} archived archived 
     * @param {UserMinimal} creator creator 
     * @param {MyLocation} location location 
     */
    constructor(
        readonly id: string,
        readonly name: string,
        readonly content: string,
        readonly commentChannel: string,
        readonly chatChannel: string,
        readonly rankCategory: string,
        readonly thumbnail: string,
        readonly mediaString: string,
        readonly likesCount: number,
        readonly commentsCount: number,
        readonly contributorsCount: number,
        readonly createdAt: number,
        readonly updatedAt: number,
        readonly expiredAt: number,
        readonly viewsCount: number,
        readonly rank: number,
        readonly points: number,
        readonly blockedList: string[],
        readonly mediaList: string[],
        readonly tags: string[],
        readonly sources: string[],
        readonly contributors: string[],
        readonly requests: string[],
        readonly archived: boolean,
        readonly creator: UserMinimal,
        readonly location: MyLocation,
        ) {}
}
