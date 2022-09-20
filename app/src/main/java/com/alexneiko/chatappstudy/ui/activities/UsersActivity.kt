package com.alexneiko.chatappstudy.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import com.alexneiko.chatappstudy.adapters.UsersAdapter
import com.alexneiko.chatappstudy.databinding.ActivityUsersBinding
import com.alexneiko.chatappstudy.listeners.UserListener
import com.alexneiko.chatappstudy.models.User
import com.alexneiko.chatappstudy.utilites.Constants
import com.alexneiko.chatappstudy.utilites.PreferenceManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot

class UsersActivity : BaseActivity(), UserListener {

    private lateinit var binding: ActivityUsersBinding
    private lateinit var preferenceManager: PreferenceManager

    /** shared prefs */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersBinding.inflate(layoutInflater)
        preferenceManager = PreferenceManager(applicationContext) /** shared prefs init */
        setContentView(binding.root)
        getUsers() /** Get users from Firebase DB to chat */
        setListeners()
    }

    private fun setListeners() {
        binding.imageBack.setOnClickListener { onBackPressed() }

    }

    private fun getUsers() {
        loading(true)
        /** get Firebase DB */
        val database = FirebaseFirestore.getInstance()

        /** get data from Firebase DB */
        database.collection(Constants.KEY_COLLECTION_USERS)
            .get()
            .addOnCompleteListener { task ->
                loading(false)
                val currentUserId = preferenceManager.getString(Constants.KEY_USER_ID)
                if(task.isSuccessful && task.result != null) {
                    val users: MutableList<User> = mutableListOf()
                    /** For в котлине. Темповая переменная и перебирается так "темп_val IN {массив, диапазон}" */
                    for (queryDocumentSnapshot: QueryDocumentSnapshot in  task.result) {
                        /** skipping a loop IF this is Self user */
                        if (currentUserId == queryDocumentSnapshot.id) {
                            continue
                        }
                        /** Fill all users to array */
                        val user =  User()
                        user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME).toString()
                        user.email = queryDocumentSnapshot.getString(Constants.KEY_MAIL).toString()
                        user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE).toString()
                        user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN).toString()
                        user.id = queryDocumentSnapshot.id
                        users.add(user)
                    }
                    /** Set users to Recycler adapter in UI */
                    if (users.size > 0) {
                        val usersAdapter = UsersAdapter(users = users, userListener = this)
                        binding.usersRecyclerView.adapter = usersAdapter
                        binding.usersRecyclerView.visibility = View.VISIBLE
                    } else {
                        showErrorMessage()
                    }
                } else {
                    showErrorMessage()
                }


            }
    }

    private fun showErrorMessage() {
        binding.textErrorMessage.text = String.format("%s", "No user available")
        binding.textErrorMessage.visibility = View.VISIBLE
    }

    private fun loading(isLoading: Boolean) {
        if(isLoading) {
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.GONE
        }
    }

    override fun onUserClicked(user: User) {
        Log.d("alex", "choose user = ${user.name}")
        val intent = Intent(applicationContext, ChatActivity::class.java)
        intent.putExtra(Constants.KEY_USER, user)
        startActivity(intent)
        finish()
    }
}