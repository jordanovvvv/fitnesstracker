package com.example.fitnesstracker

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBar
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
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
        val reg_image = findViewById<CircleImageView>(R.id.reg_profileImage)


        reg_image.setOnClickListener {
            selectImage()
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
                        val reg_email = findViewById<EditText>(R.id.reg_profileEmail)
                        val reg_username = findViewById<EditText>(R.id.reg_profileUsername)

                        val username = reg_username.text.toString()
                        val userProfilePicture = fileUri
                        val userEmail = reg_email.text.toString()

                        val update = UserProfileChangeRequest.Builder()
                            .setDisplayName(username)
                            .setPhotoUri(userProfilePicture)
                            .build()

                        mAuth.currentUser!!.updateProfile(update)

                        Toast.makeText(
                            this, "Authentication successful.",
                            Toast.LENGTH_SHORT
                        ).show()
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    Toast.makeText(
                        this, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun selectImage(){
        ImagePicker.with(this)
            .crop()
            .compress(1024)
            .maxResultSize(1080, 1080)
            .start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val reg_image = findViewById<CircleImageView>(R.id.reg_profileImage)
        when(resultCode){
            Activity.RESULT_OK -> {
                fileUri = data?.data
                reg_image.setImageURI(fileUri)
            }
            ImagePicker.RESULT_ERROR -> {
                Toast.makeText(this, "There was an error picking the image!", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(this, "Image picking cancelled!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}