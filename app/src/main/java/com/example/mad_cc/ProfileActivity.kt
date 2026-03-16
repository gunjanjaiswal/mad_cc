package com.example.mad_cc

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var nameInput: EditText
    private lateinit var emailText: TextView
    private lateinit var emergencyInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        nameInput = findViewById(R.id.profileNameInput)
        emailText = findViewById(R.id.profileEmailText)
        emergencyInput = findViewById(R.id.emergencyNumberInput)
        val saveBtn = findViewById<Button>(R.id.saveProfileBtn)

        val user = auth.currentUser
        emailText.text = user?.email

        loadProfileData()

        saveBtn.setOnClickListener {
            saveProfileData()
        }
    }

    private fun loadProfileData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    nameInput.setText(document.getString("name"))
                    emergencyInput.setText(document.getString("emergencyContact"))
                }
            }
    }

    private fun saveProfileData() {
        val userId = auth.currentUser?.uid ?: return
        val name = nameInput.text.toString().trim()
        val emergencyContact = emergencyInput.text.toString().trim()

        if (emergencyContact.isEmpty()) {
            Toast.makeText(this, "Please enter an emergency number", Toast.LENGTH_SHORT).show()
            return
        }

        val profileData = hashMapOf(
            "name" to name,
            "emergencyContact" to emergencyContact,
            "email" to auth.currentUser?.email
        )

        db.collection("users").document(userId).set(profileData)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile Saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
