# ProGuard rules for Carventory

# Keep the Car data class
-keep class com.example.carventory.data.Car { *; }

# Keep the DAO interface
-keep interface com.example.carventory.data.CarDao { *; }

# Keep the Room database class
-keep class com.example.carventory.data.CarDatabase { *; }

# Keep the repository class
-keep class com.example.carventory.data.CarRepository { *; }

# Keep the ViewModels
-keep class com.example.carventory.viewmodels.** { *; }

# Keep the activities
-keep class com.example.carventory.** { *; }

# Keep the adapter
-keep class com.example.carventory.adapters.CarAdapter { *; }

# Keep all public methods in the classes
-keepclassmembers public class * {
    public <init>(...);
}