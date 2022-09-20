package com.alexneiko.chatappstudy.ui.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.alexneiko.chatappstudy.R
import com.alexneiko.chatappstudy.databinding.ActivitySingUpBinding
import com.alexneiko.chatappstudy.utilites.Constants
import com.alexneiko.chatappstudy.utilites.PreferenceManager
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException

class SingUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySingUpBinding
    private lateinit var preferenceManager: PreferenceManager /** shared prefs */
    private var encodedImage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingUpBinding.inflate(layoutInflater)
        preferenceManager = PreferenceManager(applicationContext) /** shared prefs init */
        setContentView(binding.root)
        setListeners()
    }

    private fun setListeners() {
        binding.singInTextView.setOnClickListener {
            onBackPressed() /** обращение к стандартному стеку activity */
        }

        binding.buttonSingUp.setOnClickListener {
            if (verifyUserDataBeforeSign()) { /** validate */
                singUp()
            }
        }

        /** Choose image */
        binding.layoutImage.setOnClickListener { v ->
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            pickImage.launch(intent)
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
    }

    /** Registration in firebase */
    private fun singUp() {
        loading(true)

        /** get Firebase DB */
        val database = FirebaseFirestore.getInstance()

        /** generate  HashMap and put data */
        val user: HashMap<String, Any> = hashMapOf()
        user[Constants.KEY_NAME] = binding.inputName.text.toString()
        user[Constants.KEY_MAIL] = binding.inputEmail.text.toString()
        user[Constants.KEY_PASSWORD] = binding.inputPassword.text.toString()
        user[Constants.KEY_IMAGE] = encodedImage

        /** push data to Firebase DB */
        database.collection(Constants.KEY_COLLECTION_USERS)
            .add(user)
            .addOnSuccessListener { documentReference ->
                loading(false)
                preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true) /** set IS LOGIN to refs */
                preferenceManager.putString(Constants.KEY_USER_ID, documentReference.id) /** FIREBASE get ID */
                preferenceManager.putString(Constants.KEY_NAME, binding.inputName.text.toString())
                preferenceManager.putString(Constants.KEY_IMAGE, encodedImage)
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) /** clear backstack */
                startActivity(intent)

            }
            .addOnFailureListener { exception ->
                loading(false)
                showToast(exception.message.toString())
            }
    }


    private fun encodeImageStream(bitmap: Bitmap): String {
        val previewWidth = 150
        val previewHeight = bitmap.height * previewWidth / bitmap.width
        var previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false)
        val byteArrayOutputStream: ByteArrayOutputStream = ByteArrayOutputStream()
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val bytes: ByteArray = byteArrayOutputStream.toByteArray()

        /** Return String array form util -> "android.util.Base64 "*/
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    /** Check -> did user choose image And generate image URI */
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            if (result.data != null) {
                val imageUri = result!!.data!!.data
                try {
                    val inputStream = imageUri?.let { contentResolver.openInputStream(it) }
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    binding.imageProfile.setImageBitmap(bitmap) /** set image to UI */
                    binding.textAddImage.visibility = View.GONE
                    encodedImage = encodeImageStream(bitmap) /** convert image to string array for save */

                } catch (exception: FileNotFoundException) {
                    exception.printStackTrace()
                }
            }
        }

    }

    private fun verifyUserDataBeforeSign(): Boolean {
        if (encodedImage.isEmpty()) {
            showToast(getString(R.string.alert_msg_select_image))
            return false
        } else if (binding.inputName.text.toString().trim().isEmpty()) {
            showToast(getString(R.string.alert_msg_enter_name))
            return false
        }
        else if (binding.inputEmail.text.toString().trim().isEmpty()) {
            showToast(getString(R.string.alert_msg_enter_email))
            return false
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.text.toString()).matches()) {
            showToast(getString(R.string.alert_msg_valid_email))
            return false
        }
        else if (binding.inputPassword.text.toString().trim().isEmpty()) {
            showToast(getString(R.string.alert_msg_enter_pass))
            return false
        }
        else if (binding.inputConfirmPassword.text.toString().trim().isEmpty()) {
            showToast(getString(R.string.alert_msg_confirm_pass))
            return false
        }
        else if (!binding.inputPassword.text.toString().equals(binding.inputConfirmPassword.text.toString())) {
            showToast(getString(R.string.alert_msg_same_pass))
            return false
        }
        else return true
    }

    /** Animate loading in UI for different Views (look for XML -> android:animateLayoutChanges="true" in FrameLayout */
    private fun loading(isLoading: Boolean) {
        if(isLoading) {
            binding.buttonSingUp.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.buttonSingUp.visibility = View.VISIBLE
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

}