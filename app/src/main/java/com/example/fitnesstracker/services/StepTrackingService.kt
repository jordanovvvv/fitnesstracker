package com.example.fitnesstracker.services

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import com.example.fitnesstracker.models.DailySteps
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StepTrackingService : LifecycleService() {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var lastSavedDate = ""
    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate() {
        super.onCreate()
        Log.d("StepTrackingService", "Service started")

        lastSavedDate = getTodayDate()
        adjustStepsFromLastEntry()

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                checkEndOfDay()
                handler.postDelayed(this, 3600000)
            }
        }, 3600000)
    }

    private fun checkEndOfDay() {
        val currentDate = getTodayDate()

        if (currentDate != lastSavedDate) {
            saveStepsToDatabase()
            resetSteps()
            lastSavedDate = currentDate
        }
    }

    private fun getTodayDate(): String {
        return dateFormat.format(Date())
    }

    private fun saveStepsToDatabase() {
        val steps = getCurrentSteps()
        val dailySteps = DailySteps(
            displayname = mAuth.currentUser?.displayName.toString(),
            date = lastSavedDate,
            steps = steps.toString()
        )
        saveStepsToFirestore(dailySteps)
    }

    private fun createInitialEntry() {
        val dailySteps = DailySteps(
            displayname = mAuth.currentUser?.displayName.toString(),
            date = getTodayDate(),
            steps = "0"
        )
        saveStepsToFirestore(dailySteps)
    }

    private fun saveStepsToFirestore(dailySteps: DailySteps) {
        db.collection("dailySteps")
            .add(dailySteps)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "Daily steps successfully saved to database!", Toast.LENGTH_SHORT).show()
                Log.d("Firestore", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving steps to database!", Toast.LENGTH_SHORT).show()
                Log.w("Firestore", "Error adding document", e)
            }
    }

    private fun getCurrentSteps(): Int {
        // Get the current step count from SharedPreferences or your data source
        val sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE)
        return sharedPreferences.getInt("currentSteps", 0)
    }

    private fun resetSteps() {
        val sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("currentSteps", 0)
        editor.apply()
        Log.d("StepTrackingService", "Steps reset to 0")
    }

    fun adjustStepsFromLastEntry() {
        val currentUser = mAuth.currentUser?.displayName ?: return

        db.collection("dailySteps")
            .whereEqualTo("displayname", currentUser)
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val lastEntry = documents.documents[0].toObject(DailySteps::class.java)

                    val lastEntryDate = lastEntry?.date ?: getTodayDate()
                    val currentDate = getTodayDate()

                    if (lastEntryDate == currentDate) {
                        val savedSteps = lastEntry?.steps?.toIntOrNull() ?: 0
                        updateCurrentSteps(savedSteps)
                        Log.d("StepTrackingService", "Loaded today's existing steps: $savedSteps")
                    } else {
                        resetSteps()
                        Log.d("StepTrackingService", "Started new day with 0 steps")
                    }
                } else {
                    resetSteps()
                    createInitialEntry()
                    Log.d("StepTrackingService", "No previous entries found, starting with 0 steps")
                }
            }
            .addOnFailureListener { e ->
                Log.e("StepTrackingService", "Error getting last entry", e)
                resetSteps()
            }
    }

    private fun updateCurrentSteps(steps: Int) {
        val sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("currentSteps", steps)
        editor.apply()
        Log.d("StepTrackingService", "Updated current steps to: $steps")
    }

}