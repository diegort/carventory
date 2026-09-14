package es.dmontesinos.android.carventory.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
                val maxHeight = 960
                val maxWidth = 1280

                // Glide decodes directly at the requested size (rather than
                // decoding the full-resolution image first) and corrects for
                // EXIF orientation automatically, so there's no need to manage
                // BitmapFactory sampling or orientation math by hand.
                val bitmap = Glide.with(requireContext())
                    .asBitmap()
                    .load(uri)
                    .fitCenter()
                    .submit(maxWidth, maxHeight)
                    .get()

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
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}