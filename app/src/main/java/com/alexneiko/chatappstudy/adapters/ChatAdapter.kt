package com.alexneiko.chatappstudy.adapters


import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alexneiko.chatappstudy.databinding.ItemContainerRecivedMessageBinding
import com.alexneiko.chatappstudy.databinding.ItemContainerSentMessageBinding
import com.alexneiko.chatappstudy.models.ChatMessage
import com.alexneiko.chatappstudy.utilites.Constants

class ChatAdapter (_chatMessages: List<ChatMessage>, _receivedProfileImage: Bitmap, _senderId: String):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val chatMessages = _chatMessages
    private var receivedProfileImage = _receivedProfileImage
    private val senderId = _senderId

    fun setReceiverProfileImage(bitmap: Bitmap) {
        receivedProfileImage = bitmap
    }


    class SendMessageViewHolder(_binding: ItemContainerSentMessageBinding): RecyclerView.ViewHolder(_binding.root) {
        private val binding = _binding

        fun setData(chatMessage: ChatMessage) {
            binding.messageTextView.text = chatMessage.message
            binding.dateTimeTextView.text = chatMessage.dateTime
        }
    }

    class ReceivedMessageViewHolder(_binding: ItemContainerRecivedMessageBinding): RecyclerView.ViewHolder(_binding.root) {
        private val binding = _binding

        fun setData(chatMessage: ChatMessage, receivedProfileImage: Bitmap) {
            binding.messageTextView.text = chatMessage.message
            binding.dateTimeTextView.text = chatMessage.dateTime
            if (receivedProfileImage != null) {
                binding.imageProfile.setImageBitmap(receivedProfileImage)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == Constants.VIEW_TYPE_SEND) {
            return SendMessageViewHolder(ItemContainerSentMessageBinding
                .inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            return  ReceivedMessageViewHolder(
                ItemContainerRecivedMessageBinding
                .inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == Constants.VIEW_TYPE_SEND) {
            val holder = holder as SendMessageViewHolder
            holder.setData(chatMessages[position])
        } else {
            val holder = holder as ReceivedMessageViewHolder
            holder.setData(chatMessages[position], receivedProfileImage)
        }
    }

    override fun getItemCount(): Int {
        return chatMessages.size
    }

    override fun getItemViewType(position: Int): Int {
        return if(chatMessages[position].senderId.equals(senderId))
            Constants.VIEW_TYPE_SEND
        else Constants.VIEW_TYPE_RECEIVED
    }
}