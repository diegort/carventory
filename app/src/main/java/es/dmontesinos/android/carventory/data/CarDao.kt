// CarDao.kt
package es.dmontesinos.android.carventory.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CarDao {
    @Query("SELECT * FROM cars ORDER BY name ASC")
    fun getAllCars(): LiveData<List<Car>>

    @Query("SELECT * FROM cars WHERE id = :id")
    fun getCar(id: Long): LiveData<Car>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(car: Car): Long

    @Update
    suspend fun update(car: Car)

    @Delete
    suspend fun delete(car: Car)
}