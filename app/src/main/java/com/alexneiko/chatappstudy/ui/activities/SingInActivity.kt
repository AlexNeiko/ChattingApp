package com.alexneiko.chatappstudy.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.alexneiko.chatappstudy.R
import com.alexneiko.chatappstudy.databinding.ActivitySingInBinding
import com.alexneiko.chatappstudy.utilites.Constants
import com.alexneiko.chatappstudy.utilites.PreferenceManager
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class SingInActivity : AppCompatActivity() {

    lateinit var binding: ActivitySingInBinding
    private lateinit var preferenceManager: PreferenceManager  /** shared prefs */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingInBinding.inflate(layoutInflater)
        preferenceManager = PreferenceManager(applicationContext) /** shared prefs init */
        setContentView(binding.root)

        setListeners()

        /** Start Main screen if user already login in past. (best practices = -> Launcher Activity and routing */
        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) /** clear backstack */
            startActivity(intent)

        }
    }

    private fun setListeners() {
        binding.createNewAccountTextView.setOnClickListener {
            startActivity(Intent(applicationContext, SingUpActivity::class.java))
        }

        binding.buttonSingIn.setOnClickListener {
            if (isValidSingInDetails()) {
                singIn()
            }
        }

    }


    private fun singIn() {
        loading(true)

        /** get Firebase DB */
        val database = FirebaseFirestore.getInstance()

        /** push data to Firebase DB */
        database.collection(Constants.KEY_COLLECTION_USERS)
             /** Verify, if User create in DB */
            .whereEqualTo(Constants.KEY_MAIL, binding.inputEmail.text.toString())
            .whereEqualTo(Constants.KEY_PASSWORD, binding.inputPassword.text.toString())
            .get()
            .addOnCompleteListener { task ->
                /** Check -> is response is not Empty  */
                if (task.isSuccessful && task.result != null && task.result.documents.size > 0) {
                    /** GET user from DB */
                    val documentSnapshot: DocumentSnapshot = task.result.documents.get(0)
                    /** Put login flag and user id to local prefs */
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true)
                    preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.id)
                    preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME).toString())
                    preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE).toString())

                    val intent = Intent(applicationContext, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) /** clear backstack */
                    startActivity(intent)
                } else {
                    loading(false)
                    showToast("Unable to sing in :(")
                }
            }
            .addOnFailureListener { exception ->
                loading(false)
                showToast(exception.message.toString())
            }
    }


    /** Animate loading in UI for different Views (look for XML -> android:animateLayoutChanges="true" in FrameLayout */
    private fun loading(isLoading: Boolean) {
        if(isLoading) {
            binding.buttonSingIn.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.buttonSingIn.visibility = View.VISIBLE
            binding.progressBar.visibility = View.INVISIBLE
        }
    }


    private fun showToast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
    }

    private fun isValidSingInDetails(): Boolean {
        if(binding.inputEmail.text.toString().trim().isEmpty()) {
            showToast("Enter Email")
            return false
        } else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.text.toString()).matches()) {
            showToast("Enter valid Email")
            return false
        } else if (binding.inputPassword.text.toString().isEmpty()) {
            showToast("Enter password")
            return false
        } else return true
    }



    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    //TEST write to firebase !!!!!! Study this :)
    private fun addDataToFirestore() {
        val database = FirebaseFirestore.getInstance()
        //MOCKUP TEST
        val data: HashMap<String, Any> = hashMapOf()
        data.put("first_name", "Alex") /** можно так */
        data["last_name"] = "Neiko" /** а можно и так */

        /** CREATE table in Firebase */
        database.collection("users") /** PUT DATA TO FIREBASE STORAGE !!! */
            .add(data)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(applicationContext, "Data Inserted", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(applicationContext, "Error: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }


}