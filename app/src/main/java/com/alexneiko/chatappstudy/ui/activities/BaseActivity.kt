package com.alexneiko.chatappstudy.ui.activities

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.alexneiko.chatappstudy.utilites.Constants
import com.alexneiko.chatappstudy.utilites.PreferenceManager
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

open class BaseActivity: AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)


    }



    /** Update state to remote DB */
    override fun onPause() {
        super.onPause()
        val preferenceManager = PreferenceManager(applicationContext) /** shared prefs init */
        val database = FirebaseFirestore.getInstance() /** get Firebase DB */
        val documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
        documentReference.update(Constants.KEY_AVAILABILITY, 0)
    }

    /** Update state to remote DB */
    override fun onResume() {
        super.onResume()
        val preferenceManager = PreferenceManager(applicationContext) /** shared prefs init */
        val database = FirebaseFirestore.getInstance() /** get Firebase DB */
        val documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
            .document(preferenceManager.getString(Constants.KEY_USER_ID))
        documentReference.update(Constants.KEY_AVAILABILITY, 1)
    }
}