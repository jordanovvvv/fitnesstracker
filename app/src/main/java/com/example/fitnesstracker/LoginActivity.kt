package com.example.fitnesstracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.util.Base64

class LoginActivity : AppCompatActivity() {
    private lateinit var credentialManager: CredentialManager
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val WEB_CLIENT_ID = "426572815551-3tjusrgg84g8mocmdivluai4ic5epqsf.apps.googleusercontent.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        credentialManager = CredentialManager.create(this)

        val actionBar: ActionBar = supportActionBar!!
        actionBar.hide()

        val googleSignInButton = findViewById<ConstraintLayout>(R.id.googleSignInButtonLayout)
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        val email = findViewById<EditText>(R.id.idemail)
        val password = findViewById<EditText>(R.id.idpassword)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<TextView>(R.id.registerButton)

        loginButton.setOnClickListener {
            val emailValue = email.text.toString()
            val passwordValue = password.text.toString()

            if (emailValue.isNullOrEmpty() || passwordValue.isNullOrEmpty()) {
                Toast.makeText(this, "Invalid credentials!", Toast.LENGTH_LONG).show()
            } else {
                loginUser(emailValue, passwordValue)
            }
        }

        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun signInWithGoogle() {

        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .setNonce(generateNonce())
            .build()

        val googleRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@LoginActivity, googleRequest)
                handleSignIn(result)
            } catch (e: GetCredentialException) {
                handleFailure(e)
            }
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            if (!idToken.isNullOrEmpty()) {
                firebaseAuthWithGoogle(idToken)
            } else {
                Log.e("LoginActivity", "ID Token is null or empty")
            }
        } else {
            Log.e("LoginActivity", "Unexpected credential type")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateOut()
                } else {
                    Log.e("LoginActivity", "Firebase authentication failed: ${task.exception?.message}")
                    Toast.makeText(this, "Firebase authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleFailure(e: GetCredentialException) {
        Log.e("LoginActivity", "Credential fetching failed", e)
        Toast.makeText(this, "Sign-in failed", Toast.LENGTH_SHORT).show()
    }

    private fun generateNonce(length: Int = 32): String {
        val secureRandom = SecureRandom()
        val randomBytes = ByteArray(length)
        secureRandom.nextBytes(randomBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
    }

    private fun navigateOut() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        if (checkIfLoggedIn()) {
            navigateOut()
        }
    }

    private fun loginUser(emailValue: String, passwordValue: String) {
        mAuth.signInWithEmailAndPassword(emailValue, passwordValue)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateOut()
                } else {
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkIfLoggedIn(): Boolean {
        return mAuth.currentUser != null
    }
}
