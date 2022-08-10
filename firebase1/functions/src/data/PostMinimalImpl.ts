import { MyLocation } from "./MyLocation";
import { UserMinimal } from "./UserMinimal";

/**
 * Implementation class of post minimal interface
 * 
 * Minified post
 */
export class PostMinimalImpl {
    /**
     * 
     * @param {string} objectID objectID 
     * @param {string} type type 
     * @param {string} name name 
     * @param {string} content content 
     * @param {string} thumbnail thumbnail 
     * @param {string} chatChannel chatChannel 
     * @param {number} createdAt createdAt 
     * @param {number} updatedAt updatedAt 
     * @param {number} rank rank 
     * @param {string[]} mediaList mediaList 
     * @param {string[]} tags tags 
     * @param {UserMinimal} creator creator 
     * @param {MyLocation} location location 
     */
    constructor(
        readonly objectID: string,
        readonly type: string,
        readonly name: string,
        readonly content: string,
        readonly thumbnail: string,
        readonly chatChannel: string,
        readonly createdAt: number,
        readonly updatedAt: number,
        readonly rank: number,
        readonly mediaList: string[],
        readonly tags: string[],
        readonly creator: UserMinimal,
        readonly location: MyLocation,
    ) {}
}
