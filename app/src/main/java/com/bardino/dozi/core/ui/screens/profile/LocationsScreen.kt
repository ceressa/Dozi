package com.bardino.dozi.core.ui.screens.profile

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bardino.dozi.R
import com.bardino.dozi.core.ui.theme.*
import com.bardino.dozi.geofence.GeofenceReceiver
import com.bardino.dozi.navigation.Screen
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Card


private data class SavedPlace(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val lat: Double,
    val lng: Double,
    val address: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var places by remember { mutableStateOf(listOf<SavedPlace>()) }
    var toDelete by remember { mutableStateOf<SavedPlace?>(null) }
    var showMapPicker by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    val markerState = remember { MarkerState() }

    LaunchedEffect(Unit) { isVisible = true }

    // Konum izni
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Konum izni verilmedi", Toast.LENGTH_SHORT).show()
        }
    }

    // İlk girişte
    LaunchedEffect(key1 = true) {
        delay(300)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }


    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(DoziCoral, DoziCoralDark)
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Top bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Geri",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = "Konumlarım",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Box(Modifier.size(40.dp)) // Spacer for symmetry
                    }

                    Spacer(Modifier.height(12.dp))

                    // Subtitle
                    Text(
                        text = "${places.size}/5 konum kaydedildi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        },
        floatingActionButton = {
            val canAdd = places.size < 5
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn() + fadeIn()
            ) {
                FloatingActionButton(
                    onClick = {
                        if (!canAdd) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Maksimum 5 konum ekleyebilirsiniz")
                            }
                            return@FloatingActionButton
                        }

                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        } else {
                            showMapPicker = true
                        }
                    },
                    containerColor = DoziCoral,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.shadow(8.dp, RoundedCornerShape(18.dp))
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Konum Ekle",
                        tint = Color.White
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (places.isEmpty()) {
            EmptyLocationsState(
                onAddClick = { showMapPicker = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(places, key = { it.id }) { place ->
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInVertically(
                            initialOffsetY = { 50 },
                            animationSpec = tween(300)
                        ) + fadeIn()
                    ) {
                        LocationCard(
                            place = place,
                            onDelete = { toDelete = place }
                        )
                    }
                }

                item {
                    val remaining = 5 - places.size
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = DoziCoral.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = DoziCoral
                            )
                            Text(
                                text = if (remaining > 0)
                                    "Kalan $remaining konum ekleyebilirsiniz"
                                else
                                    "Konum limiti doldu (5/5)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // Silme onayı
    toDelete?.let { deleting ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = DoziCoral,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Konumu Sil?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "“${deleting.name}” konumunu silmek istediğinize emin misiniz?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        places = places.filterNot { it.id == deleting.id }
                        toDelete = null
                        scope.launch {
                            snackbarHostState.showSnackbar("${deleting.name} silindi")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DoziCoral
                    )
                ) {
                    Text("Sil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { toDelete = null }) {
                    Text("İptal")
                }
            }
        )
    }


    // Harita picker
    if (showMapPicker) {
        MapPickerSheet(
            context = context,
            onDismiss = { showMapPicker = false },
            onConfirm = { pickedLatLng, pickedName, address ->
                if (places.size >= 5) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Maksimum 5 konum ekleyebilirsiniz")
                    }
                    return@MapPickerSheet
                }
                if (pickedLatLng == null || pickedName.isBlank()) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Konum ve isim gerekli")
                    }
                    return@MapPickerSheet
                }

                // 🔹 Yeni konumu kaydet
                val newPlace = SavedPlace(
                    name = pickedName.trim(),
                    lat = pickedLatLng.latitude,
                    lng = pickedLatLng.longitude,
                    address = address
                )
                places = places + newPlace

                // 🔹 Jeofence kur
                addGeofence(context, newPlace.name, newPlace.lat, newPlace.lng)

                showMapPicker = false
                scope.launch {
                    snackbarHostState.showSnackbar("📍 ${pickedName} kaydedildi ve etkinleştirildi")
                }
            }
        )
    }
}

@Composable
private fun EmptyLocationsState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Float animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.dozi_idea),
            contentDescription = null,
            modifier = Modifier
                .size(160.dp)
                .offset(y = floatOffset.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Henüz konum eklemedin",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,

            color = MaterialTheme.colorScheme.onSurface,


            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Ev, iş, okul gibi sık gittiğin yerleri ekle. Bu konumlara vardığında ilaç hatırlatması yapalım! 📍",
            style = MaterialTheme.typography.bodyLarge,

            color = MaterialTheme.colorScheme.onSurfaceVariant,

            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onAddClick,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DoziCoral
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                "İlk Konumu Ekle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LocationCard(
    place: SavedPlace,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(DoziCoral.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = DoziCoral,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (place.address.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = place.address,
                        style = MaterialTheme.typography.bodySmall,

                        color = MaterialTheme.colorScheme.onSurfaceVariant,

                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "📍 ${String.format("%.4f", place.lat)}, ${String.format("%.4f", place.lng)}",
                    style = MaterialTheme.typography.labelSmall,

                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)


                )
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(40.dp)
                    .background(DoziCoral.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Sil",
                    tint = DoziCoral
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapPickerSheet(
    context: Context,
    onDismiss: () -> Unit,
    onConfirm: (LatLng?, String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var picked by remember { mutableStateOf<LatLng?>(null) }
    var name by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val cameraState = rememberCameraPositionState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val markerState = remember { MarkerState() }



    // Başlangıç konumu
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    cameraState.position = CameraPosition.fromLatLngZoom(
                        LatLng(loc.latitude, loc.longitude),
                        14f
                    )
                }
                isLoading = false
            }.addOnFailureListener {
                isLoading = false
            }
        } catch (e: Exception) {
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(DoziCoral, DoziCoralDark)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "Konum Seç",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Haritadan bir yer seç ve isimlendir",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Adres veya yer ara...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = DoziCoral)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            scope.launch {
                                try {
                                    val geocoder = Geocoder(context, Locale.getDefault())
                                    val results = geocoder.getFromLocationName(searchQuery, 1)
                                    if (!results.isNullOrEmpty()) {
                                        val loc = results[0]
                                        val pos = LatLng(loc.latitude, loc.longitude)
                                        cameraState.position = CameraPosition.fromLatLngZoom(pos, 15f)
                                        picked = pos
                                        address = loc.getAddressLine(0) ?: ""
                                    } else {
                                        Toast.makeText(context, "Sonuç bulunamadı", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Arama hatası", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Ara", tint = DoziCoral)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DoziCoral,
                    focusedLabelColor = DoziCoral
                )
            )

            // Map
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                GoogleMap(
                    modifier = Modifier.matchParentSize(),
                    cameraPositionState = cameraState,
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = true,
                        zoomControlsEnabled = false
                    ),
                    properties = MapProperties(
                        isMyLocationEnabled = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ),
                    onMapClick = { latLng ->
                        markerState.position = latLng
                        picked = latLng
                    }
                ) {
                    Marker(
                        state = markerState,
                        title = "Seçilen konum"
                    )
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DoziCoral)
                    }
                }
            }

            // Name input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Yer Adı") },
                placeholder = { Text("Örn: Evim, İşyerim, Okulum") },
                leadingIcon = {
                    Icon(Icons.Default.Label, contentDescription = null, tint = DoziCoral)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DoziCoral,
                    focusedLabelColor = DoziCoral
                )
            )

            // Address display
            if (address.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DoziCoral.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = DoziCoral,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, DoziCoral)
                ) {
                    Text("Vazgeç", color = DoziCoral, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onConfirm(picked, name, address)
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    enabled = picked != null && name.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DoziCoral
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Kaydet", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
fun addGeofence(context: Context, id: String, lat: Double, lng: Double) {
    val geofence = Geofence.Builder()
        .setRequestId(id)
        .setCircularRegion(lat, lng, 150f) // 150 metre yarıçap
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
        .build()

    val geofencingRequest = com.google.android.gms.location.GeofencingRequest.Builder()
        .setInitialTrigger(com.google.android.gms.location.GeofencingRequest.INITIAL_TRIGGER_ENTER)
        .addGeofence(geofence)
        .build()

    val intent = Intent(context, GeofenceReceiver::class.java)
    val pendingIntent = android.app.PendingIntent.getBroadcast(
        context,
        id.hashCode(),
        intent,
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
    )



    val geofencingClient = com.google.android.gms.location.LocationServices.getGeofencingClient(context)
    geofencingClient.addGeofences(geofencingRequest, pendingIntent)
        .addOnSuccessListener {
            android.widget.Toast.makeText(context, "📍 $id konumu eklendi", android.widget.Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener {
            android.widget.Toast.makeText(context, "Geofence kurulamadı: ${it.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
}
