package com.example.fitnesstracker.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fitnesstracker.R
import com.example.fitnesstracker.databinding.FragmentProfileBinding
import com.example.fitnesstracker.models.Checkings
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.tapadoo.alerter.Alerter
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*


class ProfileFragment : Fragment() {

    val mAuth = FirebaseAuth.getInstance()
    val database = Firebase.database
    val checkingsReference = database.getReference("checkings")

    private lateinit var profileViewModel: ProfileViewModel
    private var _binding: FragmentProfileBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        profileViewModel =
            ViewModelProvider(this).get(ProfileViewModel::class.java)

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root


        val profileImage = root.findViewById<CircleImageView>(R.id.profileImage)
        profileImage.setImageURI(mAuth.currentUser?.photoUrl)

        val profileName = root.findViewById<TextView>(R.id.profileName)
        profileName.setText(mAuth.currentUser?.displayName)

        val heightID = root.findViewById<TextView>(R.id.heightID)
        val weightID = root.findViewById<TextView>(R.id.weightID)


        val c = Calendar.getInstance().time
        val df = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        val formattedDate = df.format(c)

        val button = root.findViewById<TextView>(R.id.hwButtonId)
        button.setOnClickListener {
            Alerter.Companion.create(activity)
                .setTitle("You want to track your stats?")
                .setText("Click me to set record your weight and height!")
                .setIcon(R.drawable.ic_alert)
                .setBackgroundColorRes(R.color.nice_blue)
                .setOnClickListener(View.OnClickListener {
                    val builder = AlertDialog.Builder(requireActivity())
                    val inflater = layoutInflater
                    val dialogLayout = inflater.inflate(R.layout.weight_height_layout, null)
                    val height = dialogLayout.findViewById<EditText>(R.id.set_height)
                    val weight = dialogLayout.findViewById<EditText>(R.id.set_weight)

                    with(builder){
                        setTitle("Enter your height and weight respectively:")
                        setPositiveButton("Confirm"){dialog, which ->
                            heightID.setText(height.text.toString() + " cm")
                            weightID.setText(weight.text.toString() + " kg")
                            val height1: String = heightID.text.toString()
                            val weight1: String = weightID.text.toString()
                            uploadData(formattedDate, height1, weight1)

                            Alerter.Companion.create(activity)
                                .setTitle("Success!")
                                .setText("You have successfully set your height and weight!")
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


        return root
    }
    private fun uploadData(date: String, height: String, weight: String) {
        val currentChecking = Checkings(mAuth.currentUser!!.displayName!!, date, height, weight)

        checkingsReference.push().setValue(currentChecking)
            .addOnCompleteListener(OnCompleteListener<Void?> { task ->
                if (task.isSuccessful) {

                } else {
                    Toast.makeText(
                        activity,
                        "Error: " + task.exception!!.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}