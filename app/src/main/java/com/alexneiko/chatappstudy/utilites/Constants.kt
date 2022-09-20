package com.alexneiko.chatappstudy.utilites

object Constants {

    const val KEY_COLLECTION_USERS = "users"
    const val KEY_NAME = "name"
    const val KEY_MAIL = "mail"
    const val KEY_PASSWORD = "password"
    const val KEY_PREFERENCE_NAME = "chatAppPreference"
    const val KEY_IS_SIGNED_IN = "isSignedIn"
    const val KEY_USER_ID = "userId"
    const val KEY_IMAGE = "image"
    const val KEY_FCM_TOKEN = "fcmToken"
    const val KEY_USER = "user"
    const val VIEW_TYPE_SEND = 1
    const val VIEW_TYPE_RECEIVED = 2
    const val KEY_COLLECTION_CHAT = "chat"
    const val KEY_SENDER_ID = "senderId"
    const val KEY_RECEIVER_ID = "receiverId"
    const val KEY_SENDER_NAME = "senderName"
    const val KEY_RECEIVER_NAME = "receiverName"
    const val KEY_MESSAGE = "message"
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_SENDER_IMAGE = "senderImage"
    const val KEY_RECEIVER_IMAGE = "receiverImage"
    const val KEY_LAST_MESSAGE = "lastMessage"
    const val KEY_COLLECTION_CONVERSATIONS = "conversations"
    const val KEY_AVAILABILITY = "availability"

    /** for generating HTTP REST remote request headers */
    const val REMOTE_MSG_AUTHORIZATION = "Authorization" /** CANONICAL HTTP JSON HEADER NAMING  */
    const val REMOTE_MSG_CONTENT_TYPE = "Content-Type" /** CANONICAL HTTP JSON HEADER NAMING  */
    const val REMOTE_MSG_DATA = "data" /** CANONICAL HTTP JSON HEADER NAMING  */
    const val REMOTE_MSG_REGISTRATION_IDS = "registration_ids" /** CANONICAL HTTP JSON HEADER NAMING  */

    private val remoteMsgHeaders: HashMap<String, String> = hashMapOf()

    @JvmName("getRemoteMsgHeaders1")
    fun getRemoteMsgHeaders(): HashMap<String, String> {
        if (remoteMsgHeaders.isNullOrEmpty()) {
            remoteMsgHeaders!!.put(
                REMOTE_MSG_AUTHORIZATION,
                "key=PASTE_YOUR_HERE")
            remoteMsgHeaders!!.put(REMOTE_MSG_CONTENT_TYPE, "application/json")
        }
        return remoteMsgHeaders
    }

}
