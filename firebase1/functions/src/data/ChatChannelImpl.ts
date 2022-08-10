import { Message } from "./Message";
import { UserMinimal } from "./UserMinimal";

/**
 * Implementation class of ChatChannel
 * 
 * Chat channel is the group where multiple users can communicate with each other
 */
export class ChatChannelImpl {
    /**
     * 
     * @param {string} chatChannelId The id of the chat channel
     * @param {string} postId The respective post id of the chat channel
     * @param {string} postTitle The title of the post this chat belongs to
     * @param {string} postImage Single image of the post that will represent this chat
     * @param {boolean} contributorsCount The count of all the contributors in the post
     * @param {string[]} administrators The list of admins in the group
     * @param {string[]} contributors List of all contributors in the group
     * @param {string} rules Rules of the chat
     * @param {number} createdAt Time of creation of this chat channel
     * @param {number} updatedAt Time of updation of this chat channel
     * @param {string[]} tokens Registration tokens of all the contributors
     * @param {string[]} blockedUsers List of users who are not allowed to join in the group
     * @param {boolean} archived State whether the chat channel is open or closed
     * @param {Message} lastMessage The last message of the chat channel
     */
    constructor(
        readonly chatChannelId: string,
        readonly postId: string,
        readonly postTitle: string,
        readonly postImage: string,
        readonly type: string,
        readonly rules: string,
        readonly administrators: string[],
        readonly contributors: string[],
        readonly tokens: string[],
        readonly blockedUsers: string[],
        readonly contributorsCount: number,
        readonly createdAt: number,
        readonly updatedAt: number,
        readonly mute: boolean,
        readonly archived: boolean,
        readonly authorized: boolean,
        readonly data1?: UserMinimal,
        readonly data2?: UserMinimal,
        readonly lastMessage?: Message,
        ) {}
}
