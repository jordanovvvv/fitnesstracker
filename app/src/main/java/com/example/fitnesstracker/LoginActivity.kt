package com.example.fitnesstracker

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.fitnesstracker.databinding.ActivityLoginBinding
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File

class LoginActivity : AppCompatActivity() {
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private var fileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        val actionBar: ActionBar = supportActionBar!!
        actionBar.hide()

        val layout = findViewById<ConstraintLayout>(R.id.loginlayout)

        val animationdrawable: AnimationDrawable = layout.background as AnimationDrawable
        animationdrawable.setEnterFadeDuration(1500)
        animationdrawable.setExitFadeDuration(3000)
        animationdrawable.start()

        val email = findViewById<EditText>(R.id.idemail)
        val password = findViewById<EditText>(R.id.idpassword)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<TextView>(R.id.registerButton)



        loginButton.setOnClickListener {
            val emailValue: String = email.text.toString()
            val passwordValue: String = password.text.toString()

            if (emailValue.isNullOrEmpty() || passwordValue.isNullOrEmpty()) {
                Toast.makeText(
                    this,
                    "User not registered or check your e-mail and password again!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                loginUser(emailValue, passwordValue)
            }
        }
            registerButton.setOnClickListener {
                val intent = Intent(this, RegisterActivity::class.java)
                startActivity(intent)
            }


    }



    private fun loginUser(emailValue: String, passwordValue: String) {
        mAuth.signInWithEmailAndPassword(emailValue, passwordValue)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateOut()

                } else {
                    Toast.makeText(
                        this, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            }
    }

    override fun onStart() {
        super.onStart()
        if(checkIfLoggedIn()){
            navigateOut()
        }

    }
    private fun checkIfLoggedIn(): Boolean{
        return mAuth.currentUser != null
    }

    private fun navigateOut() {
        val intent: Intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
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

    
