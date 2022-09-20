package com.alexneiko.chatappstudy.ui.activities

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import com.alexneiko.chatappstudy.adapters.RecentConversionsAdapter
import com.alexneiko.chatappstudy.databinding.ActivityMainBinding
import com.alexneiko.chatappstudy.listeners.ConversionListener
import com.alexneiko.chatappstudy.models.ChatMessage
import com.alexneiko.chatappstudy.models.User
import com.alexneiko.chatappstudy.utilites.Constants
import com.alexneiko.chatappstudy.utilites.PreferenceManager
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.messaging.FirebaseMessaging
import java.util.*
import kotlin.collections.HashMap

class MainActivity : BaseActivity(), ConversionListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferenceManager: PreferenceManager /** shared prefs */
    private lateinit var conversations: MutableList<ChatMessage>
    private lateinit var conversationAdapter: RecentConversionsAdapter
    private lateinit var database: FirebaseFirestore /** get Firebase DB */


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        preferenceManager = PreferenceManager(applicationContext) /** shared prefs init */
        setContentView(binding.root)
        init()
        loadUserDetails()
        getToken() /** при каждом заходе обновляем FCM token в FirebaseMessaging */
        setListeners()
        listenConversations() /** Добавляем слушатель обновлений базы из firebase. Внутри него код обновляет UI и адаптеры recycler */
    }


    private fun init() {
        conversations = mutableListOf()
        conversationAdapter = RecentConversionsAdapter(conversations, this) /** передаем наследованный интерфейс кликов в адаптер*/
        binding.conversionsRecyclerView.adapter = conversationAdapter
        database = FirebaseFirestore.getInstance()
    }


    private fun setListeners() {
        binding.imageSingOut.setOnClickListener {
            singOut()
        }

        binding.fabNewChat.setOnClickListener {
            startActivity(Intent(applicationContext, UsersActivity::class.java))
        }

    }

    private fun loadUserDetails() {
        binding.textName.text = preferenceManager.getString(Constants.KEY_NAME)

        /** decode image from string */
        val bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        binding.imageProfile.setImageBitmap(bitmap)
    }

    private fun showToast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
    }

    /** Добавляем слушатель обновлений базы из firebase. Внутри него код обновляет UI и адаптеры recycler */
    private fun listenConversations() {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
            .addSnapshotListener(eventListener)
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
            .addSnapshotListener(eventListener)

    }


    /** ////////////////////////////////////////// */
    /** переменная eventListener DB Firebase. Потом добавить ее когда делаешь запрос к базе -> .addSnapshotListener(eventListener) */
    private val eventListener =
        EventListener { value: QuerySnapshot?, error: FirebaseFirestoreException? ->
            if (error !=  null) { return@EventListener }
            if (value != null) {
                for(documentChange: DocumentChange in value.documentChanges) {
                    /** ПО ТАКОМУ ТИПУ МЫ ПОЛУЧИМ КРАЙНЮЮ ДОБАВЛЕННУЮ ЗАПИСЬ В БД FIREBASE -> REAL TIME LISTENER */
                    /** Тут можно чекать разные типы документов в работе firebase */
                    if (documentChange.type == DocumentChange.Type.ADDED) {
                        val senderId: String? = documentChange.document.getString(Constants.KEY_SENDER_ID)
                        val receiverId: String? = documentChange.document.getString(Constants.KEY_RECEIVER_ID)
                        val chatMessage = ChatMessage()
                        chatMessage.senderId = senderId
                        chatMessage.receiverId = receiverId
                        if (preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)) {
                            chatMessage.conversionImage = documentChange.document.getString(Constants.KEY_RECEIVER_IMAGE)
                            chatMessage.conversionName = documentChange.document.getString(Constants.KEY_RECEIVER_NAME)
                            chatMessage.conversionId = documentChange.document.getString(Constants.KEY_RECEIVER_ID)
                        } else {
                            chatMessage.conversionImage = documentChange.document.getString(Constants.KEY_SENDER_IMAGE)
                            chatMessage.conversionName = documentChange.document.getString(Constants.KEY_SENDER_NAME)
                            chatMessage.conversionId = documentChange.document.getString(Constants.KEY_SENDER_ID)
                        }
                        chatMessage.message = documentChange.document.getString(Constants.KEY_LAST_MESSAGE)
                        chatMessage.dateObject = documentChange.document.getDate(Constants.KEY_TIMESTAMP)
                        conversations.add(chatMessage)
                    /** ПО ТАКОМУ ТИПУ МЫ ПОЛУЧИМ МОДИФИЦИРОВАНННУЮ ДОБАВЛЕННУЮ ЗАПИСЬ В БД FIREBASE -> REAL TIME LISTENER */
                    } else if (documentChange.type == DocumentChange.Type.MODIFIED) {
                        for (i in 0 until conversations.size) {
                            val senderId: String? = documentChange.document.getString(Constants.KEY_SENDER_ID)
                            val receiverId: String? = documentChange.document.getString(Constants.KEY_RECEIVER_ID)
                            if(conversations[i].senderId.equals(senderId) && conversations[i].receiverId.equals(receiverId)) {
                                conversations[i].message = documentChange.document.getString(Constants.KEY_LAST_MESSAGE)
                                conversations[i].dateObject = documentChange.document.getDate(Constants.KEY_TIMESTAMP)
                                break
                            }
                        }
                    }
                }

                /** Лямбда которая сравнивает и сортирует объекты в массиве (регулярка) */
                Collections.sort(conversations) { dateObject, dateObject1 ->
                    dateObject.dateObject!!.compareTo(dateObject1.dateObject)
                }
                conversationAdapter.notifyDataSetChanged()
                binding.conversionsRecyclerView.smoothScrollToPosition(0)
                binding.conversionsRecyclerView.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            }
        }







    /** обновляем токен и вешаем слушатель (при каждом заходе */
    private fun getToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener(this::updateToken)
    }

    /** ПРИ КАЖДОМ ЛОГИНЕ ГЕНЕРИТСЯ FCM TOKEN ЧТО ЗАЛОГИНИЛСЯ. ЛЕЖИТ РЯДОМ С USER. ЕСЛИ LOGOUT -> УДАДЯЕМ FCM = FieldValue.delete()*/
    /** Rewrite login token to db */
    private fun updateToken(token: String) {
        preferenceManager.putString(Constants.KEY_FCM_TOKEN, token)
        val database = FirebaseFirestore.getInstance()   /** get Firebase DB */

        val documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
            .document(preferenceManager.getString(Constants.KEY_USER_ID))
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
            .addOnFailureListener { e -> showToast("Unable to update Token") }
    }

    private fun singOut() {
        showToast("Singing out...")
        val database = FirebaseFirestore.getInstance()   /** get Firebase DB */

        val documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))

        /** получается мы удаляем токен логина из firebase */
        val updates: HashMap<String, Any> = hashMapOf()
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete())

        ////////////////////////////////////////////////////////////////////////////////////
        /** FieldValue.delete() = мы как бы вставляем служебную команду в hashMap и пушим. */
        /** Значит удалить токен логина */
        ////////////////////////////////////////////////////////////////////////////////////


        /** получается мы удаляем токен логина из firebase */
        documentReference.update(updates) /** в hashmap команда на удаление записи */
            .addOnSuccessListener { unused ->
                preferenceManager.clear() /** удаляем токен залогиненного пользователя локально  */
                val intent = Intent(applicationContext, SingInActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) /** clear backstack */
                startActivity(intent)
            }
            .addOnFailureListener { e -> showToast("Unable to sing out") }
    }

    override fun onConversionClicked(user: User) {
        val intent = Intent(applicationContext, ChatActivity::class.java)
        intent.putExtra(Constants.KEY_USER, user)
        startActivity(intent)

    }

}