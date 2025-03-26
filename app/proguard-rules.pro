# ProGuard rules for Carventory

# Keep the Car data class
-keep class es.dmontesinos.android.carventory.data.Car { *; }

# Keep the DAO interface
-keep interface es.dmontesinos.android.carventory.data.CarDao { *; }

# Keep the Room database class
-keep class es.dmontesinos.android.carventory.data.CarDatabase { *; }

# Keep the repository class
-keep class es.dmontesinos.android.carventory.data.CarRepository { *; }

# Keep the ViewModels
-keep class es.dmontesinos.android.carventory.viewmodels.** { *; }

# Keep the activities
-keep class es.dmontesinos.android.carventory.** { *; }

# Keep the adapter
-keep class es.dmontesinos.android.carventory.adapters.CarAdapter { *; }

# Keep all public methods in the classes
-keepclassmembers public class * {
    public <init>(...);
}