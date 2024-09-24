package com.example.nativeres

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.nativeres.composables.CameraPreviewScreen
import com.example.nativeres.ui.theme.NativeResTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Composable
    fun Form(photoPath: String) {
        val context = LocalContext.current
        val databaseHelper = remember { FormDataDatabaseHelper(context) }
        Log.d("databasepogg", "${databaseHelper.getAllFormData()}")

        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var comment by remember { mutableStateOf("") }
        var currentLocation by remember { mutableStateOf("Latitude: , Longitude: ") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.padding(top = 16.dp),
                label = { Text("Name") }
            )
            TextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.padding(top = 16.dp),
                label = { Text("Email") }
            )
            TextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier.padding(top = 16.dp),
                label = { Text("Comment") }
            )
            Button(
                onClick = {
                    databaseHelper.insertFormData(name, email, comment, photoPath)
                    Toast.makeText(context, "Form data saved!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Submit")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (areLocationPermissionsGranted()) {
                        getCurrentLocation { latitude, longitude ->
                            currentLocation = "Latitude: $latitude, Longitude: $longitude"
                        }
                    } else {
                        requestLocationPermissions()
                    }
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Get Current Location")
            }

            Text(text = currentLocation, modifier = Modifier.padding(top = 16.dp))
        }
    }

    private val cameraPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                setCameraPreview()
            }
        }

    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getCurrentLocation { latitude, longitude -> }
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) -> {
                setCameraPreview()
            }
            else -> {
                cameraPermissionRequest.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun setCameraPreview() {
        setContent {
            NativeResTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var photoPath by remember { mutableStateOf<String?>(null) }

                    if (photoPath == null) {
                        CameraPreviewScreen { capturedPath ->
                            photoPath = capturedPath
                        }
                    } else {
                        Form(photoPath!!)
                    }
                }
            }
        }
    }

    private fun areLocationPermissionsGranted(): Boolean {
        return (ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestLocationPermissions() {
        locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun getCurrentLocation(onLocationFetched: (Double, Double) -> Unit) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions()
            return
        }

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                Log.d("Location", "Latitude: $latitude, Longitude: $longitude")
                onLocationFetched(latitude, longitude)
            } else {
                Log.d("Location", "No location found")
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
