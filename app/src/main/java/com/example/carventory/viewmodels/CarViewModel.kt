// CarViewModel.kt
package com.example.carventory.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.example.carventory.data.Car
import com.example.carventory.data.CarDatabase
import com.example.carventory.data.CarRepository
import kotlinx.coroutines.launch

class CarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CarRepository
    val allCars: LiveData<List<Car>>

    init {
        val carDao = CarDatabase.getDatabase(application).carDao()
        repository = CarRepository(carDao)
        allCars = repository.allCars
    }

    fun getCar(id: Long): LiveData<Car> {
        return repository.getCar(id)
    }

    fun insert(car: Car): Long {
        var newId = 0L
        viewModelScope.launch {
            newId = repository.insert(car)
        }
        return newId
    }

    fun update(car: Car) = viewModelScope.launch {
        repository.update(car)
    }

    fun delete(car: Car) = viewModelScope.launch {
        repository.delete(car)
    }
}