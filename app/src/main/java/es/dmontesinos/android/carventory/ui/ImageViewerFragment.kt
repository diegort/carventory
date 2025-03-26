package es.dmontesinos.android.carventory.ui

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
import androidx.lifecycle.lifecycleScope
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        // Hide the app bar when showing the image viewer
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        // Hide system bars using the modern approach
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // For API 30+ (Android 11+)
            requireActivity().window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // For older versions
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    )
        }

        // Load image with SubsamplingScaleImageView
        val uri = Uri.parse(args.imageUri)

        // For file URIs
        if (uri.scheme == "file" || uri.scheme == "content") {
            binding.zoomableImageView.setImage(ImageSource.uri(uri))
        } else {
            // For network images, you need to download first
            // Use Glide to handle the download to a temporary file
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val future = Glide.with(requireContext())
                            .downloadOnly()
                            .load(uri)
                            .submit()

                        val file = future.get()

                        withContext(Dispatchers.Main) {
                            binding.zoomableImageView.setImage(ImageSource.uri(file.path))
                        }
                    } catch (e: Exception) {
                        Log.e("ImageViewer", "Error loading image", e)
                    }
                }
            }
        }

        // Configure the view
        binding.zoomableImageView.apply {
            // Set minimum scale to fill screen in at least one dimension
            setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
            // Enable zooming with double tap
            setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER)
            // Enable pan
            setPanEnabled(true)
            // Enable zoom
            setZoomEnabled(true)
            // If you want to allow zooming beyond the bounds of the screen
            setMaxScale(5f) // Allow zooming up to 5x
        }

        // Close button click listener
        binding.closeButton.setOnClickListener {
            findNavController().navigateUp()
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