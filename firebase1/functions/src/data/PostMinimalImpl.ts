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
     * @param {string} objectID Id of the post for algolia
     * @param {string} type [post] [required for algolia]
     * @param {string} name Name of the post
     * @param {string} content Content of the post
     * @param {number} createdAt Time of creation of the post
     * @param {UserMinimal} creator Creator of the post
     * @param {string[]} images Images of the post
     * @param {MyLocation} location Location of the post
     * @param {string[]} tags Related tags of the post
     * @param {number} updatedAt Time of updation of the post
     */
    constructor(
        readonly objectID: string,
        readonly type: string,
        readonly name: string,
        readonly content: string,
        readonly createdAt: number,
        readonly creator: UserMinimal,
        readonly images: string[],
        readonly location: MyLocation,
        readonly tags: string[],
        readonly updatedAt: number) {}
}
