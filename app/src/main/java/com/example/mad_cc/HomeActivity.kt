package com.example.mad_cc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var medicineRecyclerView: RecyclerView
    private lateinit var adapter: MedicineAdapter
    private val medicineList = mutableListOf<Medicine>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        medicineRecyclerView = findViewById(R.id.medicineRecyclerView)
        val addMedicineBtn = findViewById<Button>(R.id.addMedicineBtn)

        adapter = MedicineAdapter(medicineList) { medicine ->
            val intent = Intent(this, MedicineActivity::class.java)
            intent.putExtra("EDIT_MEDICINE", medicine)
            startActivity(intent)
        }

        medicineRecyclerView.layoutManager = LinearLayoutManager(this)
        medicineRecyclerView.adapter = adapter

        addMedicineBtn.setOnClickListener {
            startActivity(Intent(this, MedicineActivity::class.java))
        }

        loadMedicinesFromFirestore()
    }

    private fun loadMedicinesFromFirestore() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("medicines")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                medicineList.clear()
                if (value != null) {
                    for (doc in value.documents) {
                        val medicine = doc.toObject(Medicine::class.java)
                        if (medicine != null) {
                            medicineList.add(medicine)
                        }
                    }
                }
                adapter.notifyDataSetChanged()
            }
    }
}
