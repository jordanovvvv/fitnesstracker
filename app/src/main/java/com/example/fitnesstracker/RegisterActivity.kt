package com.example.fitnesstracker

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.actionCodeSettings
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception

class RegisterActivity : AppCompatActivity() {
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private var fileUri: Uri? = null
    private lateinit var reg_image: CircleImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val layout = findViewById<RelativeLayout>(R.id.registerlayout)
        val animationdrawable: AnimationDrawable = layout.background as AnimationDrawable
        animationdrawable.setEnterFadeDuration(1500)
        animationdrawable.setExitFadeDuration(3000)
        animationdrawable.start()
        val actionBar: ActionBar = supportActionBar!!
        actionBar.hide()

        val button_saveinfo = findViewById<Button>(R.id.button_profileSaveInfo)
        val button_cancel = findViewById<TextView>(R.id.button_cancel)

        val reg_email = findViewById<EditText>(R.id.reg_profileEmail)
        val reg_username = findViewById<EditText>(R.id.reg_profileUsername)
        val reg_password = findViewById<EditText>(R.id.reg_profilePassword)
        reg_image = findViewById(R.id.reg_profileImage)

        val imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            uri?.let {
                fileUri = it
                reg_image.setImageURI(fileUri) // Update the image view
            }
        }

        reg_image.setOnClickListener {
            // Launch the image picker
            imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        button_cancel.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        button_saveinfo.setOnClickListener {
            saveUserInfo()
            val emailValue: String = reg_email.text.toString()
            val passwordValue: String = reg_password.text.toString()

            if (emailValue.isNullOrEmpty() || passwordValue.isNullOrEmpty()) {
                Toast.makeText(
                    this,
                    "Plase fill in the email and password information correctly!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                registerUser(emailValue, passwordValue)
                val intent = intent
                val emailLink = intent.data.toString()

                if (mAuth.isSignInWithEmailLink(emailLink)) {
                    val emailValue: String = reg_email.text.toString()
                    mAuth.signInWithEmailLink(emailValue, emailLink)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Check if email is verified
                                mAuth.currentUser?.reload()?.addOnCompleteListener {
                                    if (mAuth.currentUser?.isEmailVerified == true) {
                                        Toast.makeText(
                                            this,
                                            "Successfully signed in with a verified email!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "Please verify your email first.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        mAuth.signOut() // Log out if not verified
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    this,
                                    "Error signing in with verified email! Try again!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                }

            }
        }



    }

    private fun setUserInfo() {
        val navView: NavigationView = findViewById(R.id.nav_view)
        val headerView: View = navView.getHeaderView(0)

        val profilePicture = headerView.findViewById<CircleImageView>(R.id.profilePicture)
        val profileName = headerView.findViewById<TextView>(R.id.tv_profileName)
        val profileEmail = headerView.findViewById<TextView>(R.id.tv_profileEmail)

        profileEmail.setText(mAuth.currentUser?.email)
        profileName.setText(mAuth.currentUser?.uid)
        profilePicture.setImageURI(mAuth.currentUser?.photoUrl)

        fileUri = mAuth.currentUser?.photoUrl
    }

    private fun saveUserInfo(){
        val reg_email = findViewById<EditText>(R.id.reg_profileEmail)
        val reg_username = findViewById<EditText>(R.id.reg_profileUsername)

        mAuth.currentUser?.let {
            val userProfilePicture = fileUri
            val userEmail = reg_email.text.toString()

            val update = UserProfileChangeRequest.Builder()
                .setDisplayName(reg_username.text.toString())
                .setPhotoUri(userProfilePicture)
                .build()

            GlobalScope.launch(Dispatchers.IO){
                try{
                    it.updateProfile(update).await()
                    it.updateEmail(userEmail)
                    withContext(Dispatchers.Main){
                        setUserInfo()

                        Toast.makeText(
                            this@RegisterActivity,
                            "Profile succesfully updated!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }catch (e: Exception){
                    withContext(Dispatchers.Main){
                        Toast.makeText(
                            this@RegisterActivity,
                            e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun registerUser(emailValue: String, passwordValue: String) {
        mAuth.createUserWithEmailAndPassword(emailValue, passwordValue)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    mAuth.currentUser?.let {
                        // Send email verification link
                        val actionCodeSettings = actionCodeSettings {
                            url = "https://fitnesstracker.page.link/verify?mode=action&oobCode=code"
                            handleCodeInApp = true
                            setIOSBundleId("com.example.ios")
                            setAndroidPackageName("com.example.android", true, "12")
                        }

                        mAuth.sendSignInLinkToEmail(emailValue, actionCodeSettings)
                            .addOnCompleteListener { emailTask ->
                                if (emailTask.isSuccessful) {
                                    Log.d(TAG, "Verification email sent to $emailValue.")
                                    Toast.makeText(
                                        this, "Verification email sent. Please verify before logging in.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Log.e(TAG, "Failed to send verification email.", emailTask.exception)
                                    Toast.makeText(
                                        this, "Failed to send verification email.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                        // Sign out the user to ensure they aren't automatically logged in
                        mAuth.signOut()

                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    Log.e(TAG, "User creation failed.", task.exception)
                    Toast.makeText(
                        this, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}