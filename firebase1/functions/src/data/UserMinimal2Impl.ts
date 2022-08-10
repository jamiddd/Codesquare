import { MyLocation } from "./MyLocation";

/**
 * Implementation class of user minimal 2 interface
 * 
 * Minified User
 */
export class UserMinimal2Impl {
    /**
     * 
     * @param {string} objectID Id of the user for algolia search
     * @param {string} email Email of the user
     * @param {string} about About the user
     * @param {number} createdAt Time of creation
     * @param {string[]} interests Interests of this user
     * @param {MyLocation} location Location of this user
     * @param {string} name Name of the user
     * @param {number} premiumState State of premium
     * @param {string} tag Tag of the user
     * @param {string} username Username of the user
     * @param {string} type [user] [For algolia]
     * @param {string?} photo Profile photo of the user
     */
    constructor(
        readonly objectID: string,
        readonly email: string,
        readonly about: string,
        readonly createdAt: number,
        readonly interests: string[],
        readonly location: MyLocation,
        readonly name: string,
        readonly premiumState: number,
        readonly tag: string,
        readonly username: string,
        readonly type: string,
        readonly photo?: string,) {}
}
