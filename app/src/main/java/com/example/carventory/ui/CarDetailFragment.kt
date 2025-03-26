package com.example.carventory.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.carventory.R
import com.example.carventory.data.Car
import com.example.carventory.databinding.FragmentCarDetailBinding
import com.example.carventory.viewmodels.CarViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CarDetailFragment : Fragment() {

    private var _binding: FragmentCarDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CarViewModel by viewModels()
    private val args: CarDetailFragmentArgs by navArgs()
    private var currentCar: Car? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCarDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupMenu()

        binding.carDetailImage.setOnClickListener {
            currentCar?.let { car ->
                car.imageUri?.let { imageUri ->
                    if (imageUri.isNotEmpty()) {
                        val action = CarDetailFragmentDirections.actionCarDetailFragmentToImageViewerFragment(imageUri)
                        findNavController().navigate(action)
                    }
                }
            }
        }
    }

    private fun setupObservers() {
        val carId = args.carId
        viewModel.getCar(carId).observe(viewLifecycleOwner) { car ->
            car?.let {
                currentCar = it
                updateUI(it)
            }
        }
    }

    private fun updateUI(car: Car) {
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = car.name
        }

        Glide.with(requireContext())
            .load(car.imageUri)
            .placeholder(R.drawable.ic_car_placeholder)
            .error(R.drawable.ic_car_placeholder)
            .into(binding.carDetailImage)
    }

    private fun setupMenu() {
        val menuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.car_detail_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_edit -> {
                        currentCar?.let {
                            findNavController().navigate(
                                CarDetailFragmentDirections.actionCarDetailFragmentToCarFormFragment(it.id)
                            )
                        }
                        true
                    }
                    R.id.action_delete -> {
                        showDeleteConfirmationDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(getString(R.string.delete_confirmation))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                currentCar?.let {
                    viewModel.delete(it)
                    findNavController().navigateUp()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}