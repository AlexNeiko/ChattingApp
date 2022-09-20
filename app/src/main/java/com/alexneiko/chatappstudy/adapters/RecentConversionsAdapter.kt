package com.alexneiko.chatappstudy.adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alexneiko.chatappstudy.databinding.ItemContainerRecentConversionsBinding
import com.alexneiko.chatappstudy.listeners.ConversionListener
import com.alexneiko.chatappstudy.models.ChatMessage
import com.alexneiko.chatappstudy.models.User

class RecentConversionsAdapter(_chatMessages: List<ChatMessage>, _conversionListener: ConversionListener)
    : RecyclerView.Adapter<RecentConversionsAdapter.ConversionsViewHolder>() {

    private val chatMessages = _chatMessages
    private val conversionListener = _conversionListener /** слушатель кликов в активити */

    class ConversionsViewHolder(_binding: ItemContainerRecentConversionsBinding): RecyclerView.ViewHolder(_binding.root) {
        private val binding = _binding

        fun setData(chatMessage: ChatMessage, conversionListener: ConversionListener) {
            binding.imageProfile.setImageBitmap(getUserImage(chatMessage.conversionImage.toString()))
            binding.textName.text = chatMessage.conversionName
            binding.texRecentMessage.text = chatMessage.message
            /** слушатель кликов в активити */
            binding.root.setOnClickListener {
                val user = User()
                user.id = chatMessage.conversionId.toString()
                user.name = chatMessage.conversionName.toString()
                user.image = chatMessage.conversionImage.toString()
                conversionListener.onConversionClicked(user)
            }
        }


        private fun getUserImage(encodedImage: String): Bitmap {
            val bytes: ByteArray = Base64.decode(encodedImage, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversionsViewHolder {
        val itemContainerRecentConversionsBinding: ItemContainerRecentConversionsBinding = ItemContainerRecentConversionsBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return ConversionsViewHolder(itemContainerRecentConversionsBinding)
    }

    override fun onBindViewHolder(holder: ConversionsViewHolder, position: Int) {
        holder.setData(chatMessages[position], conversionListener)
    }

    override fun getItemCount(): Int {
        return chatMessages.size
    }


}