package es.dmontesinos.android.carventory.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.core.view.doOnPreDraw
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import es.dmontesinos.android.carventory.R
import es.dmontesinos.android.carventory.data.Car
import es.dmontesinos.android.carventory.databinding.FragmentCarFormBinding
import es.dmontesinos.android.carventory.viewmodels.CarViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CarFormFragment : Fragment() {

    private var _binding: FragmentCarFormBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CarViewModel by viewModels()
    private val args: CarFormFragmentArgs by navArgs()

    private var currentCar: Car? = null
    private var selectedImageUri: Uri? = null
    private var imageUri: Uri? = null
    private var saveMenuItem: MenuItem? = null

    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            dispatchTakePictureIntent()
        } else {
            Toast.makeText(requireContext(), getString(R.string.camera_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            imageUri?.let { uri ->
                selectedImageUri = uri
                Glide.with(requireContext())
                    .load(uri)
                    .placeholder(R.drawable.ic_car_placeholder)
                    .into(binding.carImagePreview)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition =
            TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCarFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedImageUri?.let {
            outState.putString("selectedImageUri", it.toString())
        }

        imageUri?.let {
            outState.putString("imageUri", it.toString())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        savedInstanceState?.getString("selectedImageUri")?.let {
            selectedImageUri = it.toUri()
            binding.carImagePreview.setImageURI(selectedImageUri)
        }

        savedInstanceState?.getString("imageUri")?.let {
            imageUri = it.toUri()
        }

        setupFormMode()
        setupButtons()
        setupMenu()
    }

    private fun setupFormMode() {
        val carId = args.carId
        if (carId != -1L) {
            // Edit mode
            (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.edit_car)
            viewModel.getCar(carId).observe(viewLifecycleOwner) { car ->
                car?.let {
                    currentCar = it
                    binding.carNameInput.setText(it.name)

                    Glide.with(requireContext())
                        .load(it.imageUri)
                        .placeholder(R.drawable.ic_car_placeholder)
                        .into(binding.carImagePreview)
                }
            }
        } else {
            // Add mode
            (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.add_car)
        }
    }

    private fun setupButtons() {
        binding.addImageButton.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.car_form_menu, menu)
                saveMenuItem = menu.findItem(R.id.action_save)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_save -> {
                        saveCar()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun saveCar() {
        val name = binding.carNameInput.text.toString().trim()

        if (name.isEmpty()) {
            binding.nameLayout.error = getString(R.string.name_required)
            return
        }

        saveMenuItem?.isEnabled = false

        lifecycleScope.launch {
            val imageUriString: String? = when {
                selectedImageUri != null -> {
                    val success = compressImage(selectedImageUri!!)
                    if (success) {
                        selectedImageUri.toString()
                    } else {
                        // Compression failed, show error and re-enable button
                        withContext(Dispatchers.Main) {
                            saveMenuItem?.isEnabled = true
                        }
                        null
                    }
                }
                currentCar != null -> currentCar?.imageUri ?: ""
                else -> ""
            }

            if (imageUriString == null) {
                return@launch
            }

            if (imageUriString.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), getString(R.string.please_take_picture), Toast.LENGTH_SHORT).show()
                    saveMenuItem?.isEnabled = true
                }
                return@launch
            }

            // Delete old image if we are editing and a new image was taken
            if (currentCar != null && selectedImageUri != null && currentCar?.imageUri?.isNotEmpty() == true) {
                val oldImagePath = Uri.parse(currentCar?.imageUri).path
                oldImagePath?.let {
                    withContext(Dispatchers.IO) {
                        val oldFile = File(it)
                        if (oldFile.exists()) {
                            oldFile.delete()
                        }
                    }
                }
            }


            val message: String
            if (currentCar != null) {
                // Update existing car
                val updatedCar = currentCar!!.copy(name = name, imageUri = imageUriString)
                viewModel.update(updatedCar)
                message = getString(R.string.car_updated)
            } else {
                // Add new car
                viewModel.insert(Car(name = name, imageUri = imageUriString))
                message = getString(R.string.new_car_added)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                dispatchTakePictureIntent()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show an explanation to the user
                Toast.makeText(requireContext(),
                    getString(R.string.camera_permission_denied),
                    Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        // Create a file for the image
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Toast.makeText(requireContext(), getString(R.string.error_creating_image_file), Toast.LENGTH_SHORT).show()
            null
        }

        // Continue only if the file was successfully created
        photoFile?.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                requireContext(),
                "es.dmontesinos.android.carventory.fileprovider",
                it
            )
            imageUri = photoURI
            cameraLauncher.launch(photoURI)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    private suspend fun compressImage(uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val targetSizeBytes = 200 * 1024 // 200KB
                var imageStream = requireContext().contentResolver.openInputStream(uri)

                // Get EXIF orientation
                val exifInterface = imageStream?.let { ExifInterface(it) }
                val orientation = exifInterface?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                imageStream?.close()

                // Decode bounds first so we can downsample directly instead of
                // decoding the full-resolution image and scaling afterwards
                val maxHeight = 960
                val maxWidth = 1280

                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                imageStream = requireContext().contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(imageStream, null, boundsOptions)
                imageStream?.close()

                boundsOptions.inSampleSize = calculateInSampleSize(boundsOptions, maxWidth, maxHeight)
                boundsOptions.inJustDecodeBounds = false

                imageStream = requireContext().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(imageStream, null, boundsOptions)
                imageStream?.close()

                if (bitmap == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), getString(R.string.error_compressing_image), Toast.LENGTH_LONG).show()
                    }
                    return@withContext false
                }

                // Correct the bitmap according to the EXIF orientation. Camera apps
                // (Pixel's included) can tag portrait photos with any of the 8 EXIF
                // orientation values, not just simple 90/180/270 rotations - handling
                // only those left flipped/transposed images uncorrected.
                val correctedBitmap = applyExifOrientation(bitmap, orientation ?: ExifInterface.ORIENTATION_NORMAL)

                // Resize the bitmap to the final target size
                val ratio: Float = Math.min(maxWidth.toFloat() / correctedBitmap.width, maxHeight.toFloat() / correctedBitmap.height)
                val newWidth = Math.round(ratio * correctedBitmap.width)
                val newHeight = Math.round(ratio * correctedBitmap.height)

                val scaledBitmap = Bitmap.createScaledBitmap(correctedBitmap, newWidth, newHeight, true)

                val outputStream = requireContext().contentResolver.openOutputStream(uri, "w")
                if (outputStream == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), getString(R.string.error_compressing_image), Toast.LENGTH_LONG).show()
                    }
                    return@withContext false
                }

                val baos = ByteArrayOutputStream()
                var quality = 90
                do {
                    baos.reset()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                    quality -= 5
                } while (baos.size() > targetSizeBytes && quality > 40)

                outputStream.write(baos.toByteArray())
                outputStream.close()
                baos.close()

                true
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), getString(R.string.error_compressing_image), Toast.LENGTH_LONG).show()
                }
                false
            }
        }
    }

    private fun applyExifOrientation(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return source
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // The EXIF orientation isn't applied until after decoding, so the raw
            // decoded bounds may have width/height swapped relative to the final
            // display orientation. Sample against the larger of the two target
            // dimensions to stay safe for both landscape- and portrait-tagged images.
            val reqSize = Math.max(reqWidth, reqHeight)
            while (halfHeight / inSampleSize >= reqSize || halfWidth / inSampleSize >= reqSize) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}