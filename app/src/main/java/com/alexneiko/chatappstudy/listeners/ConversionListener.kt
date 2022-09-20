package com.alexneiko.chatappstudy.listeners

import com.alexneiko.chatappstudy.models.User

interface ConversionListener {
    fun onConversionClicked(user: User)
}