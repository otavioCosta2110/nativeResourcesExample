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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.nativeres.ui.theme.NativeResTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Composable
    fun Form(photoPath: String?) {
        var showDialog by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val databaseHelper = remember { FormDataDatabaseHelper(context) }

        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var comment by remember { mutableStateOf("") }
        var currentLocation by remember { mutableStateOf("Please click the button above to get your current location") }

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
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
                label = { Text("Comment") }
            )

            Text("Photo Path: ${photoPath ?: "No photo taken."}")

            Button(
                onClick = {
                    setCameraPreview()
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Open Camera")
            }

            Button(
                onClick = {
                    databaseHelper.insertFormData(name, email, comment, photoPath!!)
                    Toast.makeText(context, "Form data saved!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.padding(top = 16.dp),
                colors = ButtonColors(
                    containerColor = Color.Green,
                    contentColor = Color.Black,
                    disabledContentColor = Color.Gray,
                    disabledContainerColor = Color.LightGray

                )
            ) {
                Text("Submit")
            }

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

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    showDialog = true
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Check Database")
            }

            if (showDialog) {
                showDatabaseDialog(databaseHelper) {
                    showDialog = false
                }
            }
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
            }
            else -> {
                cameraPermissionRequest.launch(Manifest.permission.CAMERA)
            }
        }

        setContent {
            NativeResTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var photoPath by remember { mutableStateOf<String?>(null) }
                    Form(photoPath)
                }
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
                Log.d("Location", "No location found. Please enable location services on your phone.")
                Toast.makeText(this, "No location found. Please enable location services on your phone.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Composable
    private fun showDatabaseDialog(databaseHelper: FormDataDatabaseHelper, onDismiss: () -> Unit) {
        val data = databaseHelper.getAllFormData().joinToString("\n")

        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text("Database Entries") },
            text = { Text(data.ifEmpty { "No entries found." }) },
            confirmButton = {
                Button(onClick = { onDismiss() }) {
                    Text("OK")
                }
            }
        )
    }
}

