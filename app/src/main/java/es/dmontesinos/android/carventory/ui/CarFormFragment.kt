package es.dmontesinos.android.carventory.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import es.dmontesinos.android.carventory.R
import es.dmontesinos.android.carventory.data.Car
import es.dmontesinos.android.carventory.databinding.FragmentCarFormBinding
import es.dmontesinos.android.carventory.viewmodels.CarViewModel
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
            // Delete old image if one exists and this is an edit operation
            if (currentCar != null && currentCar?.imageUri != null && currentCar?.imageUri?.isNotEmpty() == true) {
                val oldImagePath = Uri.parse(currentCar?.imageUri).path
                oldImagePath?.let {
                    val oldFile = File(it)
                    if (oldFile.exists()) {
                        oldFile.delete()
                    }
                }
            }

            // Set the new image
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

        binding.saveButton.setOnClickListener {
            saveCar()
        }
    }

    private fun saveCar() {
        val name = binding.carNameInput.text.toString().trim()

        if (name.isEmpty()) {
            binding.nameLayout.error = getString(R.string.name_required)
            return
        }

        val imageUriString = when {
            selectedImageUri != null -> selectedImageUri.toString()
            currentCar != null -> currentCar?.imageUri ?: ""
            else -> {
                Toast.makeText(requireContext(), getString(R.string.please_take_picture), Toast.LENGTH_SHORT).show()
                return
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
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}