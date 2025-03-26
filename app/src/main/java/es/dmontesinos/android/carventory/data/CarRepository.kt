// CarRepository.kt
package es.dmontesinos.android.carventory.data

import androidx.lifecycle.LiveData

class CarRepository(private val carDao: CarDao) {
    val allCars: LiveData<List<Car>> = carDao.getAllCars()

    fun getCar(id: Long): LiveData<Car> {
        return carDao.getCar(id)
    }

    suspend fun insert(car: Car): Long {
        return carDao.insert(car)
    }

    suspend fun update(car: Car) {
        carDao.update(car)
    }

    suspend fun delete(car: Car) {
        carDao.delete(car)
    }
}