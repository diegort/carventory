package es.dmontesinos.android.carventory.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import es.dmontesinos.android.carventory.R
import es.dmontesinos.android.carventory.data.Car
import es.dmontesinos.android.carventory.databinding.ItemCarBinding
import es.dmontesinos.android.carventory.databinding.ItemCarListBinding

class CarAdapter(
    private var isGridView: Boolean = true,
    private val onItemClick: (Car) -> Unit
) : ListAdapter<Car, RecyclerView.ViewHolder>(CarDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_GRID = 0
        private const val VIEW_TYPE_LIST = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (isGridView) VIEW_TYPE_GRID else VIEW_TYPE_LIST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_GRID -> {
                val binding = ItemCarBinding.inflate(inflater, parent, false)
                GridViewHolder(binding, onItemClick)
            }
            else -> {
                val binding = ItemCarListBinding.inflate(inflater, parent, false)
                ListViewHolder(binding, onItemClick)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val car = getItem(position)
        when (holder) {
            is GridViewHolder -> holder.bind(car)
            is ListViewHolder -> holder.bind(car)
        }
    }

    class GridViewHolder(
        private val binding: ItemCarBinding,
        private val onItemClick: (Car) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(car: Car) {
            binding.carName.text = car.name
            // Load image with your existing image loading logic
            binding.carImage.loadImage(car.imageUri)

            binding.root.setOnClickListener {
                onItemClick(car)
            }
        }
    }

    class ListViewHolder(
        private val binding: ItemCarListBinding,
        private val onItemClick: (Car) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(car: Car) {
            binding.carNameText.text = car.name
            // Load image with your existing image loading logic
            binding.carImage.loadImage(car.imageUri)

            binding.root.setOnClickListener {
                onItemClick(car)
            }
        }
    }
}

// CarDiffCallback remains the same
class CarDiffCallback : DiffUtil.ItemCallback<Car>() {
    override fun areItemsTheSame(oldItem: Car, newItem: Car): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Car, newItem: Car): Boolean =
        oldItem == newItem
}

// Helper extension function for image loading (you should implement this based on your image loading strategy)
private fun ImageView.loadImage(uri: String?) {
    if (uri != null) {
        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.ic_car_placeholder)
            .centerCrop()
            .into(this)
    } else {
        setImageResource(R.drawable.ic_car_placeholder)
    }
}