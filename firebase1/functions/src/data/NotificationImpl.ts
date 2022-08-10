import { UserMinimal } from "./UserMinimal";

/**
 * Implementation class of the notificaion interface
 * 
 * Notification is a notifier which is used to notify a user about something
 */
export class NotificationImpl {
    /**
     * 
     * @param {string} id Id of the notification
     * @param {string} title Title of the notification
     * @param {string} content Content of the notification
     * @param {string} senderId Id of the sender
     * @param {string} receiverId Id of the receiver
     * @param {UserMinimal} sender A little packet of data about the sender
     * @param {number} createdAt Time of creation of this notification
     * @param {number} updatedAt Time of updation of this notification
     * @param {number} type Type of notificaion [-1, 0, 1]
     * @param {boolean} read State whether the notification is read or not
     * @param {string|undefined} image Associated image of this notification
     * @param {string|undefined} postId Project id if there is any associated
     * @param {string|undefined} commentChannelId Comment channel id if there is any associated
     * @param {string|undefined} commentId Comment id if there is any associated
     * @param {string|undefined} userId User is if there is any associated
     */
    constructor(
        readonly id: string,
        readonly title: string,
        readonly content: string,
        readonly senderId: string,
        readonly receiverId: string,
        readonly sender: UserMinimal,
        readonly createdAt: number,
        readonly updatedAt: number,
        readonly type: number,
        readonly read: boolean,
        readonly image?: string,
        readonly postId?: string,
        readonly commentChannelId?: string,
        readonly commentId?: string,
        readonly userId?: string,
        ) {}
}
