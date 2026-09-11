package es.dmontesinos.android.carventory.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import es.dmontesinos.android.carventory.databinding.FragmentImageViewerBinding
import android.util.Log
import android.view.MotionEvent
import androidx.lifecycle.lifecycleScope
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("ClickableViewAccessibility")
class ImageViewerFragment : Fragment() {

    private var _binding: FragmentImageViewerBinding? = null
    private val binding get() = _binding!!

    private val args: ImageViewerFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImageViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up UI appearance
        hideSystemBars()

        // Set up image loading and gestures
        loadImageWithProperRotation()
        setupSwipeToDismiss()
    }

    private fun hideSystemBars() {
        // Hide the app bar
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        // Hide system bars
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            requireActivity().window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    )
        }
    }

    private fun loadImageWithProperRotation() {
        val uri = Uri.parse(args.imageUri)

        lifecycleScope.launch {
            try {
                val rotationDegrees = withContext(Dispatchers.IO) {
                    getExifRotation(uri)
                }

                if (uri.scheme == "file" || uri.scheme == "content") {
                    binding.zoomableImageView.orientation = rotationDegrees
                    binding.zoomableImageView.setImage(ImageSource.uri(uri))
                }

                configureImageView()
            } catch (e: Exception) {
                Log.e("ImageViewer", "Error loading image: ${e.message}")
            }
        }
    }

    private fun configureImageView() {
        binding.zoomableImageView.apply {
            setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
            setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER)
            setPanEnabled(true)
            setZoomEnabled(true)
            setMaxScale(5f)
        }
    }

    private fun setupSwipeToDismiss() {
        val dismissThreshold = resources.displayMetrics.heightPixels / 4f
        val dragStartSlop = 20f
        var initialTouchX = 0f
        var initialTouchY = 0f
        var dY = 0f
        var isDragging = false

        // Listen on the image view itself: it covers the whole screen, so a
        // listener on the root would never see any touches. Taps and the
        // image view's own double-tap-to-zoom keep working because we only
        // start consuming events once a real vertical drag is detected.
        binding.zoomableImageView.setOnTouchListener { _, event ->
            // Only take over for swipe-to-dismiss when not zoomed in;
            // otherwise let the image view handle its own pan/zoom.
            val canHandleTouch = binding.zoomableImageView.isReady &&
                    binding.zoomableImageView.scale == binding.zoomableImageView.minScale

            if (!canHandleTouch) {
                isDragging = false
                return@setOnTouchListener false
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    dY = binding.root.translationY - event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isDragging) {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY
                        if (Math.abs(deltaY) > dragStartSlop && Math.abs(deltaY) > Math.abs(deltaX)) {
                            isDragging = true
                        }
                    }
                    if (isDragging) {
                        binding.root.translationY = event.rawY + dY
                        // Adjust opacity based on drag distance
                        val alpha = 1.0f - Math.min(1.0f, Math.abs(binding.root.translationY) / dismissThreshold)
                        binding.root.alpha = alpha
                    }
                    isDragging
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        if (Math.abs(binding.root.translationY) > dismissThreshold) {
                            // Dismiss with animation
                            binding.root.animate()
                                .translationY(if (binding.root.translationY > 0) binding.root.height.toFloat() else -binding.root.height.toFloat())
                                .alpha(0f)
                                .setDuration(200)
                                .withEndAction {
                                    findNavController().navigateUp()
                                }
                                .start()
                        } else {
                            // Reset position
                            binding.root.animate()
                                .translationY(0f)
                                .alpha(1.0f)
                                .setDuration(200)
                                .start()
                        }
                        isDragging = false
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
    }

    private suspend fun getExifRotation(uri: Uri): Int {
        return withContext(Dispatchers.IO) {
            try {
                requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                    val exif = androidx.exifinterface.media.ExifInterface(inputStream)
                    val orientation = exif.getAttributeInt(
                        androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                    )

                    when (orientation) {
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                } ?: 0
            } catch (e: Exception) {
                Log.e("ImageViewer", "Error reading EXIF data: ${e.message}")
                0
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Restore the app bar when leaving the fragment
        (requireActivity() as AppCompatActivity).supportActionBar?.show()

        // Show system bars using the modern approach
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // For API 30+ (Android 11+)
            requireActivity().window.insetsController?.show(WindowInsets.Type.systemBars())
        } else {
            // For older versions
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }

        _binding = null
    }
}