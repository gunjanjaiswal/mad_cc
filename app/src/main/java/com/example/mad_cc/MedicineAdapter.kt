package com.example.mad_cc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MedicineAdapter(
    private val medicines: List<Medicine>,
    private val onEditClick: (Medicine) -> Unit
) : RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder>() {

    class MedicineViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.medNameText)
        val timeText: TextView = view.findViewById(R.id.medTimeText)
        val editBtn: Button = view.findViewById(R.id.editBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicine, parent, false)
        return MedicineViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        val medicine = medicines[position]
        holder.nameText.text = medicine.name
        holder.timeText.text = String.format("Time: %02d:%02d", medicine.hour, medicine.minute)
        
        holder.editBtn.setOnClickListener { onEditClick(medicine) }
    }

    override fun getItemCount() = medicines.size
}
