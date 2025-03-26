// CarAdapter.kt
package com.example.carventory.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.carventory.R
import com.example.carventory.data.Car

class CarAdapter(private val onItemClick: (Car) -> Unit) :
    ListAdapter<Car, CarAdapter.CarViewHolder>(CarDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_car, parent, false)
        return CarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        val car = getItem(position)
        holder.bind(car)
        holder.itemView.setOnClickListener { onItemClick(car) }
    }

    class CarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.carName)
        private val carImageView: ImageView = itemView.findViewById(R.id.carImage)

        fun bind(car: Car) {
            nameTextView.text = car.name

            Glide.with(itemView.context)
                .load(car.imageUri)
                .placeholder(R.drawable.ic_car_placeholder)
                .error(R.drawable.ic_car_placeholder)
                .centerCrop()
                .into(carImageView)
        }
    }

    class CarDiffCallback : DiffUtil.ItemCallback<Car>() {
        override fun areItemsTheSame(oldItem: Car, newItem: Car): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Car, newItem: Car): Boolean {
            return oldItem == newItem
        }
    }
}