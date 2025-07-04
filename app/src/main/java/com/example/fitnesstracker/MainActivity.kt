package com.example.fitnesstracker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.credentials.ClearCredentialStateRequest
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.fitnesstracker.databinding.ActivityMainBinding
import com.example.fitnesstracker.services.NotificationService
import com.example.fitnesstracker.services.StepTrackingService
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import com.tapadoo.alerter.Alerter
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private var sensorManager: SensorManager? = null
    private var running = false
    private var totalSteps = 0f
    private var previoustotalSteps = 0f
    private var totalCalories = 0f
    private var totalDistance = 0f
    private var fileUri: Uri? = null

    private var optionsMenu: Menu? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        setContentView(binding.root)


        val navController = findNavController(R.id.nav_host_fragment_content_main)
        val fab: FloatingActionButton = binding.appBarMain.fab

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_home -> {
                    fab.visibility = View.VISIBLE // Show FAB on HomeFragment
                }
                else -> {
                    fab.visibility = View.GONE // Hide FAB on other fragments
                }
            }
        }

        loadData()
        resetSteps()

        setSupportActionBar(binding.appBarMain.toolbar)


        binding.appBarMain.fab.setOnClickListener { view ->
                Alerter.Companion.create(this)
                    .setTitle("ALERT ALERT ALERT")
                    .setText("Click me to set your daily step goal!")
                    .setIcon(R.drawable.ic_alert)
                    .setBackgroundColorRes(R.color.nice_blue)
                    .setOnClickListener(View.OnClickListener {
                        val builder = AlertDialog.Builder(this)
                        val inflater = layoutInflater
                        val dialogLayout = inflater.inflate(R.layout.edit_steps_layout, null)
                        val editSteps = dialogLayout.findViewById<EditText>(R.id.edit_steps)
                        val progressBar = findViewById<CircularProgressBar>(R.id.circularProgressBar)
                        val caloriesProgressBar = findViewById<CircularProgressBar>(R.id.circularProgressBar2)
                        val kmProgessBar = findViewById<CircularProgressBar>(R.id.kmProgressBar)

                        with(builder){
                            setTitle("Enter your daily step goal:")
                            setPositiveButton("Confirm"){dialog, which ->
                                progressBar.progressMax = editSteps.text.toString().toFloat()
                                caloriesProgressBar.progressMax = editSteps.text.toString().toFloat()*0.04.toFloat()
                                kmProgessBar.progressMax = editSteps.text.toString().toFloat()*80/100000
                                Alerter.Companion.create(this@MainActivity)
                                    .setTitle("Success!")
                                    .setText("You have successfully set your step goal!")
                                    .setIcon(R.drawable.ic_alert)
                                    .setBackgroundColorRes(R.color.nice_blue)
                                    .show()
                            }
                            setNegativeButton("Cancel"){dialog, which ->
                                Log.d("MainActivity","NegativeButtonPressed")
                            }
                            setView(dialogLayout)
                            show()
                        }
                    })
                    .show()

        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_settings, R.id.nav_profile, R.id.nav_calendar, R.id.nav_contact
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.menu.findItem(R.id.logout).setOnMenuItemClickListener {
            FirebaseAuth.getInstance().signOut()
            navigateOut()
            Toast.makeText(this, "You have been logged out.", Toast.LENGTH_LONG).show()
            true
        }

        var handler = Handler()
        var runnable: Runnable? = null

        val headerView: View = navView.getHeaderView(0)

        val username = headerView.findViewById<TextView>(R.id.tv_profileName)
        val email = headerView.findViewById<TextView>(R.id.tv_profileEmail)
        val picture = headerView.findViewById<CircleImageView>(R.id.profilePicture)

        username.setText(mAuth.currentUser?.displayName)
        email.setText(mAuth.currentUser?.email)
        mAuth.currentUser?.photoUrl?.let { uri ->
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.running_man)
                .error(R.drawable.running_man)
                .into(picture)
        }
        fileUri = mAuth.currentUser?.photoUrl
    }

    override fun onStart() {
        super.onStart()

        if(!checkIfLoggedIn()){
            navigateOut()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        val stepTrackingServiceIntent = Intent(this, StepTrackingService::class.java)
        startService(stepTrackingServiceIntent)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        optionsMenu = menu

        return true
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun checkIfLoggedIn(): Boolean{
        return mAuth.currentUser != null
    }

    private fun navigateOut() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onPause() {
        super.onPause()

        //Start notification service when app is in background
        val serviceIntent = Intent(this, NotificationService::class.java)
        startService(serviceIntent)

    }

    override fun onResume() {
        super.onResume()

        //Stop notification service when app is in foreground
        val serviceIntent = Intent(this, NotificationService::class.java)
        stopService(serviceIntent)

        running = true
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if(stepSensor == null){
            Toast.makeText(this, "No step sensor detected!", Toast.LENGTH_SHORT).show()
        } else {
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(event == null) return

        var stepstaken = findViewById<TextView>(R.id.steps_taken_id)
        val kmtaken = findViewById<TextView>(R.id.distanceID)
        var progressBar = findViewById<CircularProgressBar>(R.id.circularProgressBar)
        val caloriesProgressBar = findViewById<CircularProgressBar>(R.id.circularProgressBar2)
        val calories = findViewById<TextView>(R.id.calories_id)
        val kmProgessBar = findViewById<CircularProgressBar>(R.id.kmProgressBar)

        if(running){
            totalSteps = event!!.values[0]
            var currentSteps = totalSteps.toInt() - previoustotalSteps.toInt()
            stepstaken.text = ("$currentSteps")
            kmtaken.text = ((currentSteps.toFloat()*80)/100000).toString() + " km"
            calories.text = (currentSteps.toFloat()*0.04).toString()
            totalCalories = (currentSteps.toFloat()*0.04).toFloat()
            totalDistance = (currentSteps.toFloat()*80)/100000

            progressBar.apply {
                setProgressWithAnimation(currentSteps.toFloat())
            }
            caloriesProgressBar.apply {
                setProgressWithAnimation(totalCalories)
            }
            kmProgessBar.apply {
                setProgressWithAnimation(totalDistance)
            }

            saveData(currentSteps, totalCalories, totalDistance)
        }
    }

    private fun saveData(currentSteps: Int, totalCalories: Float, totalDistance: Float) {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("currentSteps", currentSteps)
        editor.putFloat("totalCalories", totalCalories)
        editor.putFloat("totalDistance", totalDistance)
        editor.apply()
    }

    private fun resetSteps(){

        var stepstaken = findViewById<TextView>(R.id.steps_taken_id)
        val kmtaken = findViewById<TextView>(R.id.distanceID)
        val calories = findViewById<TextView>(R.id.calories_id)
        stepstaken.setOnClickListener {
            Toast.makeText(this, "Long tap for step reset.", Toast.LENGTH_SHORT).show()
        }
        stepstaken.setOnLongClickListener {
            previoustotalSteps = totalSteps
            totalSteps = 0f
            stepstaken.text = "0"
            kmtaken.text = "0 km"
            saveData()
            calories.text = "0"
            totalCalories = 0f
            totalDistance = 0f
            true
        }
    }

    private fun saveData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("previousSteps", previoustotalSteps.toInt())
        editor.apply()
    }

    private fun loadData(){
        val sharedPreferences= getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val savedNumber = sharedPreferences.getInt("previousSteps", 0).toFloat()
        Log.d("MainActivity", "$savedNumber")
        previoustotalSteps = savedNumber
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("Sensor Accuracy", "Sensor: ${sensor?.name}, Accuracy: ${accuracy}")

        when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> {
                // High accuracy, proceed as normal
                Toast.makeText(this, "Sensor accuracy is high. Data is correct.", Toast.LENGTH_SHORT).show()
            }
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> {
                // Accuracy is acceptable, maybe show a warning
                Toast.makeText(this, "Sensor accuracy is medium.", Toast.LENGTH_SHORT).show()
            }
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> {
                // Low accuracy, consider ignoring data or recalibrating
                Toast.makeText(this, "Sensor accuracy is low, data may not be reliable.", Toast.LENGTH_SHORT).show()
            }
            SensorManager.SENSOR_STATUS_UNRELIABLE -> {
                // Data is unreliable, perhaps ignore it
                Toast.makeText(this, "Sensor accuracy is unreliable.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
