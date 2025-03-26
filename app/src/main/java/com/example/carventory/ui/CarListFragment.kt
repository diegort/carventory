package com.example.carventory.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.carventory.R
import com.example.carventory.adapter.CarAdapter
import com.example.carventory.data.Car
import com.example.carventory.databinding.FragmentCarListBinding
import com.example.carventory.viewmodels.CarViewModel

class CarListFragment : Fragment() {

    private var _binding: FragmentCarListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CarViewModel by viewModels()
    private lateinit var adapter: CarAdapter

    private var searchQuery: String = ""
    private var originalList: List<Car> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCarListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupFab()
        setupMenu()
    }

    private fun setupRecyclerView() {
        adapter = CarAdapter { car ->
            val action = CarListFragmentDirections.actionCarListFragmentToCarDetailFragment(car.id)
            findNavController().navigate(action)
        }

        // Calculate span count based on screen width
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val spanCount = (screenWidthDp / 180).toInt().coerceAtLeast(2) // 180dp minimum width per tile

        binding.carsRecyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
        binding.carsRecyclerView.adapter = adapter

        // Add item decoration for consistent spacing
        binding.carsRecyclerView.addItemDecoration(
            GridSpacingItemDecoration(spanCount, resources.getDimensionPixelSize(R.dimen.grid_spacing), true)
        )
    }

    private fun setupObservers() {
        viewModel.allCars.observe(viewLifecycleOwner) { cars ->
            cars?.let {
                // Sort the list in a case-insensitive manner
                val sortedList = it.sortedWith(compareBy { car -> car.name.lowercase() })
                originalList = sortedList
                if (searchQuery.isEmpty()) {
                    adapter.submitList(sortedList)
                    updateActionBarTitle(sortedList.size)
                } else {
                    filterCars(searchQuery)
                }
            }
        }
    }

    private fun filterCars(query: String) {
        if (query.isEmpty()) {
            adapter.submitList(originalList)
            updateActionBarTitle(originalList.size)
        } else {
            val filteredList = originalList.filter { car ->
                car.name.contains(query, ignoreCase = true)
            }
            adapter.submitList(filteredList)
            updateActionBarTitle(filteredList.size)
        }
    }

    private fun updateActionBarTitle(carCount: Int) {
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.subtitle = getString(R.string.cars_count, carCount)
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            findNavController().navigate(
                CarListFragmentDirections.actionCarListFragmentToCarFormFragment()
            )
        }
    }

    private fun setupMenu() {
        val menuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_car_list, menu)

                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem?.actionView as? SearchView

                searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return false
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        searchQuery = newText ?: ""
                        filterCars(searchQuery)
                        return true
                    }
                })

                // Restore search state if there was an active search
                if (searchQuery.isNotEmpty()) {
                    searchItem.expandActionView()
                    searchView?.setQuery(searchQuery, false)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onResume() {
        super.onResume()

        // Reset the title to "Cars" with count when returning to this fragment
        updateActionBarTitle(adapter.currentList.size)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

