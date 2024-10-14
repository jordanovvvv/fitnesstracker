package com.example.fitnesstracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
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
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.example.fitnesstracker.databinding.ActivityMainBinding
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

    private val CHANNEL_ID = "notification_channel"
    private val notificationID = 101

    private var optionsMenu: Menu? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        setContentView(binding.root)

        loadData()
        resetSteps()
        createNotificationChannel()



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
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_settings, R.id.nav_profile, R.id.nav_contact
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

        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val notifications = sp.getBoolean("notifications", true)

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(notifications){
            createNotificationChannel()
        }
        else
        {
            notificationManager.cancelAll()
        }

        var handler = Handler()
        var runnable: Runnable? = null

        val headerView: View = navView.getHeaderView(0)

        val username = headerView.findViewById<TextView>(R.id.tv_profileName)
        val email = headerView.findViewById<TextView>(R.id.tv_profileEmail)
        val picture = headerView.findViewById<CircleImageView>(R.id.profilePicture)

        username.setText(mAuth.currentUser?.displayName)
        email.setText(mAuth.currentUser?.email)
        picture.setImageURI(mAuth.currentUser?.photoUrl)
        fileUri = mAuth.currentUser?.photoUrl


    }

    private fun createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notification name"
            val descriptionText = "Notification text"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(){
        var stepstaken = findViewById<TextView>(R.id.steps_taken_id)
        val kmtaken = findViewById<TextView>(R.id.distanceID)
        val calories = findViewById<TextView>(R.id.calories_id)

        val intent: Intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingintent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val bitmapLargeIcon = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.running_man)

        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_splash_logo)
            .setContentTitle("Steps walked: " + stepstaken.text)
            .setContentText("Calories burned: " + calories.text + "kcal - Distance: " + kmtaken.text + "km")
            .setLargeIcon(bitmapLargeIcon)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Calories burned: " + calories.text + " kcal - Distance: " + kmtaken.text))
            .setContentIntent(pendingintent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)){
            notify(notificationID, builder.build())
        }

    }

    override fun onStart() {
        super.onStart()

        if(!checkIfLoggedIn()){
            navigateOut()
        }

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
        val intent: Intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()

        running = true
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if(stepSensor == null){
            Toast.makeText(this, "No step sensor detected!", Toast.LENGTH_SHORT).show()
        }else{
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
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
        }
    }
    private fun resetSteps(){

        //Notify for previous status of data
        sendNotification()

        var stepstaken = findViewById<TextView>(R.id.steps_taken_id)
        val kmtaken = findViewById<TextView>(R.id.distanceID)
        val calories = findViewById<TextView>(R.id.calories_id)
        stepstaken.setOnClickListener {
            Toast.makeText(this, "Long tap for step reset.", Toast.LENGTH_SHORT).show()
        }
        stepstaken.setOnLongClickListener {
            previoustotalSteps = totalSteps
            stepstaken.text = 0.toString()
            kmtaken.text = 0.toString() + " km"
            saveData()
            calories.text = 0.toString()
            totalCalories = 0f
            totalDistance = 0f
            true
        }
    }

    private fun saveData() {
        val sharedPreferences = getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("key1", previoustotalSteps.toInt())
        editor.apply()
    }

    private fun loadData(){
        val sharedPreferences= getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
        val savedNumber = sharedPreferences.getInt("key1", 0).toFloat()
        Log.d("MainActivity", "$savedNumber")
        previoustotalSteps = savedNumber
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}
