package es.dmontesinos.android.carventory

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import es.dmontesinos.android.carventory.databinding.ActivityMainBinding
import android.graphics.Color
import android.content.res.Configuration
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        // Replaces the deprecated setDecorFitsSystemWindows/setStatusBarColor/
        // layoutInDisplayCutoutMode combo with the version-aware AndroidX
        // helper (also draws content under the camera cutout on every
        // supported version, matching the previous SHORT_EDGES behavior).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isDarkTheme() }
        )
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // The toolbar owns the top edge: it absorbs the status bar inset.
        // The camera cutout is intentionally NOT consumed here, so the toolbar
        // and content use the screen's full width in landscape instead of
        // leaving a gap for it.
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(top = bars.top)
            insets
        }

        // Content sits below the toolbar, so it only needs the nav bar's
        // side insets (e.g. a 3-button nav bar docked on a side in landscape).
        ViewCompat.setOnApplyWindowInsetsListener(binding.navHostFragment) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, right = bars.right)
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(setOf(R.id.carListFragment))
        setupActionBarWithNavController(navController, appBarConfiguration)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Only show subtitle in CarListFragment, clear it in all other destinations
            if (destination.id != R.id.carListFragment) {
                supportActionBar?.subtitle = null
            }
        }
    }

    private fun isDarkTheme(): Boolean {
        return resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}