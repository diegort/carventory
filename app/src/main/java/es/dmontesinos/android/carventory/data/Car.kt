// Car.kt
package es.dmontesinos.android.carventory.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class Car(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val imageUri: String // Store the URI as a string
)