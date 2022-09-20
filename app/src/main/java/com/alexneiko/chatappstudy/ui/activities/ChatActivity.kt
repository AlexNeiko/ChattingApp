package com.alexneiko.chatappstudy.ui.activities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.alexneiko.chatappstudy.adapters.ChatAdapter
import com.alexneiko.chatappstudy.databinding.ActivityChatBinding
import com.alexneiko.chatappstudy.models.ChatMessage
import com.alexneiko.chatappstudy.models.User
import com.alexneiko.chatappstudy.network.ApiClient
import com.alexneiko.chatappstudy.network.ApiService
import com.alexneiko.chatappstudy.utilites.Constants
import com.alexneiko.chatappstudy.utilites.PreferenceManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : BaseActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var receivedUser: User
    private lateinit var chatMessages: MutableList<ChatMessage>
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var preferenceManager: PreferenceManager /** shared prefs */
    private lateinit var database: FirebaseFirestore /** get Firebase DB */
    private var conversionId: String? = null
    private var isReceiverAvailable: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loadReceiverDetails()
        setListeners()
        init()
        listenMessages() /** ЗАпрос чата к Firebase + добавляем слугатель. Который внури себя обновляет UI */
    }

    private fun init() {
        preferenceManager = PreferenceManager(applicationContext) /** shared prefs init */
        chatMessages = mutableListOf()
        chatAdapter = ChatAdapter(
            _chatMessages = chatMessages,
            _receivedProfileImage = getBitmapFromEncodedString(receivedUser.image)!!,
            _senderId = preferenceManager.getString(Constants.KEY_USER_ID))
        binding.chatRecyclerView.adapter = chatAdapter
        database = FirebaseFirestore.getInstance()
    }

    private fun getBitmapFromEncodedString(encodedImage: String): Bitmap? {
        if (encodedImage != null) {
            val bytes: ByteArray = Base64.decode(encodedImage, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } else {
            return null
        }

    }

    private fun sendMessage() {
        val message: HashMap<String, Any> = hashMapOf()
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
        message.put(Constants.KEY_RECEIVER_ID, receivedUser.id)
        message.put(Constants.KEY_MESSAGE, binding.inputMessageEditText.text.toString())
        message.put(Constants.KEY_TIMESTAMP, Date())
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message)
        if (conversionId != null) {
            updateConversion(binding.inputMessageEditText.text.toString())
        } else {
            val conversion: HashMap<String, Any> = hashMapOf()
            conversion.put(Constants.KEY_LAST_MESSAGE, binding.inputMessageEditText.text.toString())
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME))
            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE))
            conversion.put(Constants.KEY_RECEIVER_ID, receivedUser.id)
            conversion.put(Constants.KEY_RECEIVER_NAME, receivedUser.name)
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receivedUser.image)
            conversion.put(Constants.KEY_TIMESTAMP, Date())

            addConversion(conversion)
        }
        if (!isReceiverAvailable) {
            try {
                val tokens = JSONArray()
                tokens.put(receivedUser.token)

                val data = JSONObject()
                data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME))
                data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN))
                data.put(Constants.KEY_MESSAGE, binding.inputMessageEditText.text.toString())

                val body = JSONObject()
                body.put(Constants.REMOTE_MSG_DATA, data)
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens)

                sendNotification(body.toString())

            } catch (e: Exception) {
                showToast(e.message.toString())
            }
        }
        binding.inputMessageEditText.text = null
    }

    private fun showToast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
    }

    private fun sendNotification(messageBody: String) {
        ApiClient.retrofitClient.create(ApiService::class.java)
            .sendMessage(Constants.getRemoteMsgHeaders(), messageBody)
            .enqueue(object: Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                   if(response.isSuccessful) {
                    try {
                        if (response.body() != null) {
                            val responseJson = JSONObject(response.body())
                            val results = responseJson.getJSONArray("results")
                            if (responseJson.getInt("failure") == 1) {
                                val error = results.get(0) as JSONObject
                                showToast(error.getString("error"))
                                return
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                       showToast("Notification sent successfully")
                   } else {
                       showToast("Error: " + response.code())
                   }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                    showToast(t.message.toString())
                }

            })
    }



    //TODO РАБОТАЕТ С ПЕРВОГО РАЗА!!!! :))))))))
    private fun listenAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_USERS).document(receivedUser.id)
            .addSnapshotListener(this
            ) { value: DocumentSnapshot?, error: FirebaseFirestoreException? ->
                if (error !=  null) { return@addSnapshotListener }
                if (value != null) {
                    if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                        val availability = Objects.requireNonNull(value.getLong(Constants.KEY_AVAILABILITY))!!.toInt()
                        isReceiverAvailable = availability == 1
                    }
                    receivedUser.token = value.getString(Constants.KEY_FCM_TOKEN).toString()
                    if (receivedUser.image == null) {
                        receivedUser.image = value.getString(Constants.KEY_IMAGE).toString()
                        chatAdapter.setReceiverProfileImage(getBitmapFromEncodedString(receivedUser.image)!!)
                        chatAdapter.notifyItemRangeChanged(0, chatMessages.size)
                    }
                }
                if (isReceiverAvailable) {
                    binding.textAvailability.visibility = View.VISIBLE
                } else {
                    binding.textAvailability.visibility = View.GONE
                }
            }
    }


    private fun listenMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
            .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
            .whereEqualTo(Constants.KEY_RECEIVER_ID, receivedUser.id)
            .addSnapshotListener(eventListener) /** добавляем слугатель. Который внури себя обновляет UI */

        database.collection(Constants.KEY_COLLECTION_CHAT)
            .whereEqualTo(Constants.KEY_SENDER_ID, receivedUser.id)
            .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
            .addSnapshotListener(eventListener) /** добавляем слугатель. Который внури себя обновляет UI */
        
    }

    /** ////////////////////////////////////////// */
    /** переменная eventListener DB Firebase. Потом добавить ее когда делаешь запрос к базе -> .addSnapshotListener(eventListener) */
    private val eventListener =
        EventListener { value: QuerySnapshot?, error: FirebaseFirestoreException? ->
            if (error !=  null) { return@EventListener }
            if (value != null) {
                val count = chatMessages.size
                Log.d("alex", "value.documents.size = ${value.documents.size}")
                for (documentChange: DocumentChange in value.documentChanges) {
                    Log.d("alex" , "BOOM")
                    if(documentChange.type == DocumentChange.Type.ADDED) {
                        val chatMessage = ChatMessage()
                        chatMessage.senderId = documentChange.document.getString(Constants.KEY_SENDER_ID)
                        chatMessage.receiverId = documentChange.document.getString(Constants.KEY_RECEIVER_ID)
                        chatMessage.receiverId = documentChange.document.getString(Constants.KEY_RECEIVER_ID)
                        chatMessage.message = documentChange.document.getString(Constants.KEY_MESSAGE)
                        chatMessage.dateTime = getReadableDateTime(documentChange.document.getDate(Constants.KEY_TIMESTAMP))
                        chatMessage.dateObject = documentChange.document.getDate(Constants.KEY_TIMESTAMP)
                        chatMessages.add(chatMessage)
                    }
                }

                /** Лямбда которая сравнивает и сортирует объекты в массиве (регулярка) */
                Collections.sort(chatMessages) { dateObject, dateObject1 ->
                    dateObject.dateObject!!.compareTo(dateObject1.dateObject)
                }
                if (count == 0) {
                    chatAdapter.notifyDataSetChanged()
                } else {
                    chatAdapter.notifyItemRangeChanged(chatMessages.size, chatMessages.size)
                    binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size - 1)
                }
                binding.chatRecyclerView.visibility = View.VISIBLE
            }
            binding.progressBar.visibility = View.GONE
            /** check for new message conversions from remote firebase db */
            if(conversionId == null) { checkForConversions() }
        }

    private fun loadReceiverDetails() {
        receivedUser = intent.getSerializableExtra(Constants.KEY_USER) as User
        binding.nameTextView.text = receivedUser.name
    }

    private fun setListeners() {
        binding.imageBack.setOnClickListener { onBackPressed() }
        binding.layoutSend.setOnClickListener {
            sendMessage()
            binding.inputMessageEditText.hideKeyboard() /** extended fun for all Views -> hide keyboard on screen  */
        }

    }

    private fun getReadableDateTime(date: Date?): String {
        return SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date)
    }

    private fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun addConversion(conversion: HashMap<String, Any>) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .add(conversion)
            .addOnSuccessListener { documentReference: DocumentReference ->
                conversionId = documentReference.id
            }
    }

    private fun updateConversion(message: String) {
        val documentReference: DocumentReference =
            database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId.toString())
        documentReference.update(Constants.KEY_LAST_MESSAGE, message, Constants.KEY_TIMESTAMP, Date())
     }

    private fun checkForConversions() {
        if (chatMessages.isNotEmpty()) {
            checkForConversionRemotely(preferenceManager.getString(Constants.KEY_USER_ID), receivedUser.id)
            checkForConversionRemotely(receivedUser.id, preferenceManager.getString(Constants.KEY_USER_ID))
        }
    }

    private fun checkForConversionRemotely(senderId: String, receiverId: String) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
            .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
            .get()
            .addOnCompleteListener(conversionOnCompleteListener)
    }

    private val conversionOnCompleteListener = OnCompleteListener { task: Task<QuerySnapshot?>? ->
        if(task!!.isSuccessful && task?.result != null && task.result!!.documents.size > 0) {
            val documentSnapshot: DocumentSnapshot = task!!.result!!.documents[0]
            conversionId = documentSnapshot.id
        }
    }

    override fun onResume() {
        super.onResume()
        listenAvailabilityOfReceiver()
    }
}