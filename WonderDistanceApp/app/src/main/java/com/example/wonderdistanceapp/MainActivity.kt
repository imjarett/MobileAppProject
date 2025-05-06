package com.example.wonderdistanceapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.*
import kotlin.math.*

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            WonderDistanceAppTheme {
                var screenState by remember { mutableStateOf("welcome") }
                var userLocation by remember { mutableStateOf<Location?>(null) }
                var selectedWonder by remember { mutableStateOf<Triple<String, Double, Double>?>(null) }

                when (screenState) {
                    "welcome" -> WelcomeScreen(
                        onStart = {
                            if (ActivityCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            } else {
                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                    userLocation = location
                                    screenState = "select"
                                }
                            }
                        },
                        onExit = { finish() }
                    )

                    "select" -> userLocation?.let {
                        WonderSelectionScreen(
                            userLocation = it,
                            onWonderSelected = { wonder ->
                                selectedWonder = wonder
                                screenState = "result"
                            }
                        )
                    }

                    "result" -> {
                        if (userLocation != null && selectedWonder != null) {
                            ResultScreen(
                                userLocation = userLocation!!,
                                wonder = selectedWonder!!,
                                onRestart = { screenState = "select" },
                                onExit = { finish() }
                            )
                        }
                    }
                }
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    @Suppress("MissingPermission")
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        setContent {
                            WonderDistanceAppTheme {
                                WonderSelectionScreen(userLocation = location!!, onWonderSelected = {})
                            }
                        }
                    }
                }
            }
        }
}

@Composable
fun BackgroundWrapper(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.earth_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.2f
        )
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun WelcomeScreen(onStart: () -> Unit, onExit: () -> Unit) {
    BackgroundWrapper {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text("Welcome to Wonder Distance", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onStart) { Text("Get Started") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onExit) { Text("Exit") }
        }
    }
}

val wonders = listOf(
    Triple("Great Pyramid of Giza", 29.9792, 31.1342),
    Triple("Great Wall of China", 40.4319, 116.5704),
    Triple("Machu Picchu", -13.1631, -72.5450),
    Triple("Christ the Redeemer", -22.9519, -43.2105),
    Triple("Colosseum", 41.8902, 12.4922),
    Triple("Taj Mahal", 27.1751, 78.0421),
    Triple("Petra", 30.3285, 35.4444)
)

@Composable
fun WonderSelectionScreen(userLocation: Location, onWonderSelected: (Triple<String, Double, Double>) -> Unit) {
    val context = LocalContext.current
    val city = remember(userLocation) {
        Geocoder(context, Locale.getDefault())
            .getFromLocation(userLocation.latitude, userLocation.longitude, 1)
            ?.firstOrNull()
            ?.locality ?: "your location"
    }

    BackgroundWrapper {
        Column {
            Text(
                "Hello from $city! Pick a wonder to calculate distance:",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            wonders.forEach { wonder ->
                Button(onClick = { onWonderSelected(wonder) }, modifier = Modifier.fillMaxWidth()) {
                    Text(wonder.first)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ResultScreen(userLocation: Location, wonder: Triple<String, Double, Double>, onRestart: () -> Unit, onExit: () -> Unit) {
    val (name, lat, lon) = wonder
    val distanceKm = haversine(userLocation.latitude, userLocation.longitude, lat, lon)
    val distanceMiles = distanceKm * 0.621371

    BackgroundWrapper {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                "You are %.2f km (%.2f miles) from $name".format(distanceKm, distanceMiles),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("That's about %.0f football fields!".format(distanceMiles * 17.6))
            Spacer(modifier = Modifier.height(24.dp))
            Text("Would you like to calculate for a new location?")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRestart) { Text("Yes") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onExit) { Text("No") }
        }
    }
}

fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371 // km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}
