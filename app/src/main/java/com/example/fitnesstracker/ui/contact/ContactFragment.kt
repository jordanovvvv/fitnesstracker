package com.example.fitnesstracker.ui.contact

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.fitnesstracker.R
import com.example.fitnesstracker.databinding.FragmentContactBinding


class ContactFragment : Fragment() {

    private lateinit var contactViewModel: ContactViewModel
    private var _binding: FragmentContactBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        contactViewModel =
            ViewModelProvider(this).get(ContactViewModel::class.java)

        _binding = FragmentContactBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val callButton = root.findViewById<Button>(R.id.callButton)
        callButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:+38977111222")
            startActivity(intent)
        }

        val eFromText = root.findViewById<EditText>(R.id.eContactFrom)
        val eSubject = root.findViewById<EditText>(R.id.eSubject)
        val eMessageText = root.findViewById<EditText>(R.id.eContactMessage)

        val sendButton = root.findViewById<Button>(R.id.emailButton)
        sendButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:" + "contact@fitnesstracker.com"))
            intent.putExtra(Intent.EXTRA_SUBJECT, eSubject.text.toString())
            intent.putExtra(Intent.EXTRA_TEXT, eMessageText.text.toString())
            eFromText.setText("")
            eSubject.setText("")
            eMessageText.setText("")

            startActivity(intent)
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
