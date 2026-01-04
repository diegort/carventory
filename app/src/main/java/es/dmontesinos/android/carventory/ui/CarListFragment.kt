package es.dmontesinos.android.carventory.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import es.dmontesinos.android.carventory.R
import es.dmontesinos.android.carventory.adapters.CarAdapter
import es.dmontesinos.android.carventory.data.Car
import es.dmontesinos.android.carventory.databinding.FragmentCarListBinding
import es.dmontesinos.android.carventory.viewmodels.CarViewModel

class CarListFragment : Fragment() {

    private var _binding: FragmentCarListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CarViewModel by viewModels()
    private lateinit var adapter: CarAdapter

    private var searchQuery: String = ""
    private var originalList: List<Car> = emptyList()
    private var isGridView = true // Track current view mode

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
        adapter = CarAdapter(isGridView) { car ->
            val action = CarListFragmentDirections.actionCarListFragmentToCarDetailFragment(car.id)
            findNavController().navigate(action)
        }

        binding.carsRecyclerView.adapter = adapter
        updateLayoutManager()

        // Update item decoration based on view mode
        binding.carsRecyclerView.clearItemDecorations()
        if (isGridView) {
            // Calculate span count based on screen width
            val displayMetrics = resources.displayMetrics
            val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
            val spanCount = (screenWidthDp / 180).toInt().coerceAtLeast(2) // 180dp minimum width per tile

            binding.carsRecyclerView.addItemDecoration(
                GridSpacingItemDecoration(spanCount, resources.getDimensionPixelSize(R.dimen.grid_spacing), true)
            )
        } else {
            binding.carsRecyclerView.addItemDecoration(
                ListSpacingItemDecoration(resources.getDimensionPixelSize(R.dimen.item_spacing))
            )
        }
    }

    private fun updateLayoutManager() {
        if (isGridView) {
            // Calculate span count based on screen width
            val displayMetrics = resources.displayMetrics
            val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
            val spanCount = (screenWidthDp / 180).toInt().coerceAtLeast(2) // 180dp minimum width per tile
            binding.carsRecyclerView.layoutManager = GridLayoutManager(requireContext(), spanCount)
        } else {
            binding.carsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        }
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

                // Configure view mode toggle button
                val viewModeItem = menu.findItem(R.id.action_toggle_view)
                updateViewModeIcon(viewModeItem)

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
                return when (menuItem.itemId) {
                    R.id.action_toggle_view -> {
                        isGridView = !isGridView
                        updateViewModeIcon(menuItem)

                        // Create new adapter with new view mode
                        val currentList = adapter.currentList
                        setupRecyclerView()
                        adapter.submitList(currentList)

                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun updateViewModeIcon(menuItem: MenuItem) {
        menuItem.setIcon(
            if (isGridView) R.drawable.ic_list_view
            else R.drawable.ic_grid_view
        )
        menuItem.title = getString(
            if (isGridView) R.string.show_as_list
            else R.string.show_as_grid
        )
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

    // Extension function to clear all ItemDecorations
    private fun RecyclerView.clearItemDecorations() {
        while (itemDecorationCount > 0) {
            removeItemDecorationAt(0)
        }
    }
}
