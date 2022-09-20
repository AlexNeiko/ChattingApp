package com.alexneiko.chatappstudy.adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.alexneiko.chatappstudy.databinding.ItemContainerUserBinding
import com.alexneiko.chatappstudy.listeners.UserListener
import com.alexneiko.chatappstudy.models.User


class UsersAdapter(private val users: List<User>, private val userListener: UserListener):
    RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {



    class UserViewHolder(item: ItemContainerUserBinding): RecyclerView.ViewHolder(item.root) {
        private val item = item

        fun getUserImage(encodedImage: String): Bitmap {
            val bytes: ByteArray = Base64.decode(encodedImage, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }



        fun setUserData(user: User, listener: UserListener) {
            item.textName.text = user.name
            item.texEmail.text = user.email
            item.imageProfile.setImageBitmap(getUserImage(user.image))
            item.root.setOnClickListener { v -> listener.onUserClicked(user)}

        }

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val itemContainerUserBinding: ItemContainerUserBinding = ItemContainerUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)

        return UserViewHolder(itemContainerUserBinding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.setUserData(users[position], userListener)
    }

    override fun getItemCount(): Int {
        return users.size
    }
}