package com.alexneiko.chatappstudy.listeners

import com.alexneiko.chatappstudy.models.User

interface UserListener {
    fun onUserClicked(user: User)
}