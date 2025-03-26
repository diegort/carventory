# Carventory

Carventory is an Android application designed to manage an inventory of toy cars. The app allows users to view a list of toy cars, see details for each car, and manage their collection efficiently.

## Features

- **List of Toy Cars**: Displays a list of toy cars with their names and images.
- **Car Details**: Users can click on a car to view detailed information, including the car's name and image.
- **Local Database**: Utilizes Room for local data storage, allowing users to add, update, and delete cars from their inventory.

## Project Structure

The project is organized as follows:

```
Carventory
├── app
│   ├── src
│   │   ├── main
│   │   │   ├── java
│   │   │   │   └── com
│   │   │   │       └── example
│   │   │   │           └── carventory
│   │   │   │               ├── MainActivity.kt
│   │   │   │               ├── CarListActivity.kt
│   │   │   │               ├── CarDetailActivity.kt
│   │   │   │               ├── adapters
│   │   │   │               │   └── CarAdapter.kt
│   │   │   │               ├── data
│   │   │   │               │   ├── Car.kt
│   │   │   │               │   ├── CarDao.kt
│   │   │   │               │   ├── CarDatabase.kt
│   │   │   │               │   └── CarRepository.kt
│   │   │   │               └── viewmodels
│   │   │   │                   ├── CarListViewModel.kt
│   │   │   │                   └── CarDetailViewModel.kt
│   │   │   ├── res
│   │   │   │   ├── layout
│   │   │   │   │   ├── activity_main.xml
│   │   │   │   │   ├── activity_car_list.xml
│   │   │   │   │   ├── activity_car_detail.xml
│   │   │   │   │   └── item_car.xml
│   │   │   │   ├── values
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   └── menu
│   │   │   │       └── car_menu.xml
│   │   │   └── AndroidManifest.xml
│   │   └── test
│   │       └── java
│   │           └── com
│   │               └── example
│   │                   └── carventory
│   │                       └── ExampleUnitTest.kt
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.properties
│       └── gradle-wrapper.jar
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
└── README.md
```

## Getting Started

1. **Clone the repository**: 
   ```
   git clone https://github.com/yourusername/carventory.git
   ```

2. **Open the project** in Android Studio.

3. **Build and run** the application on an Android device or emulator.

## Dependencies

- Room for local database management
- RecyclerView for displaying the list of cars
- ViewModel and LiveData for managing UI-related data

## License

This project is licensed under the MIT License. See the LICENSE file for details.