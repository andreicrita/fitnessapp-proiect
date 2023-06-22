package com.example.fitnessapp


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.Marker
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExerciseAdapter
    private lateinit var searchEditText: EditText
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private val REQUEST_IMAGE_CAPTURE = 1
    private lateinit var cameraButton: Button
    private lateinit var welcomeMessageTextView: TextView
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var videoView: VideoView
    private lateinit var mediaController: MediaController
    private lateinit var shareButton: Button


    private var mLocationRequest: LocationRequest? = null
    private val UPDATE_INTERVAL = (10 * 1000).toLong()  // 10 secunde
    private val FASTEST_INTERVAL: Long = 2000 // 2 secunde
    private var latitude = 0.0
    private var longitude = 0.0
    private var currentLocationMarker: Marker? = null
    private lateinit var photoImageView: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // Initializare views
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        recyclerView = findViewById(R.id.recycler_view)
        adapter = ExerciseAdapter()
        welcomeMessageTextView = findViewById(R.id.welcome_message)
        mapView = findViewById(R.id.map_view)
        mapView.onCreate(savedInstanceState)
        photoImageView = findViewById(R.id.photo_image_view)
        videoView = findViewById(R.id.video_view)
        mediaController = MediaController(this)
        shareButton = findViewById(R.id.share_button)



        shareButton.setOnClickListener {
            shareImage()
        }

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                // Verificați rezultatul și procesați-l așa cum doriți
                if (data != null) {
                    val imageBitmap = data.extras?.get("data") as Bitmap?
                    val compressedBitmap = compressBitmap(imageBitmap)
                    photoImageView.setImageBitmap(compressedBitmap)
                    photoImageView.visibility = View.VISIBLE
                }
            }
        }

        // RecyclerView's layout manager si adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Setez toolbar-ul ca si action bar
        val toolbar: Toolbar = findViewById(R.id.toolbar)

        checkCameraPermission()
        cameraButton = findViewById(R.id.camera_button)
        cameraButton.setOnClickListener {
            openCamera()
        }

        // Setez drawer toggle
        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        val menu = navigationView.menu
        val homeMenuItem = menu.findItem(R.id.nav_home)
        homeMenuItem.isChecked = true

        // Setez NavigationView
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    welcomeMessageTextView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    searchEditText.visibility = View.GONE
                    cameraButton.visibility = View.GONE
                    mapView.visibility = View.GONE
                    photoImageView.visibility = View.GONE
                    videoView.visibility = View.GONE
                    shareButton.visibility = View.GONE

                }
                R.id.nav_exercises -> {
                    welcomeMessageTextView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    searchEditText.visibility = View.VISIBLE
                    cameraButton.visibility = View.GONE
                    mapView.visibility = View.GONE
                    photoImageView.visibility = View.GONE
                    videoView.visibility = View.GONE
                    shareButton.visibility = View.GONE
                }
                R.id.nav_maps -> {
                    welcomeMessageTextView.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    searchEditText.visibility = View.GONE
                    cameraButton.visibility = View.GONE
                    mapView.visibility = View.VISIBLE
                    photoImageView.visibility = View.GONE
                    videoView.visibility = View.GONE
                    shareButton.visibility = View.GONE
                }
                R.id.nav_pictures -> {
                    welcomeMessageTextView.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    searchEditText.visibility = View.GONE
                    cameraButton.visibility = View.VISIBLE
                    mapView.visibility = View.GONE
                    photoImageView.visibility = View.VISIBLE
                    videoView.visibility = View.GONE
                    shareButton.visibility = View.VISIBLE
                }
                R.id.nav_video -> {
                    welcomeMessageTextView.visibility = View.GONE
                    recyclerView.visibility = View.GONE
                    searchEditText.visibility = View.GONE
                    cameraButton.visibility = View.GONE
                    mapView.visibility = View.GONE
                    photoImageView.visibility = View.GONE
                    videoView.visibility = View.VISIBLE
                    shareButton.visibility = View.GONE
                }
            }

            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Setez datele pentru adapter
        val exercises = listOf(
            Exercise("Push-ups", "Chest, triceps, shoulders", R.drawable.push_ups),
            Exercise("Squats", "Legs, glutes", R.drawable.squats),
            Exercise("Sit-ups", "Abs", R.drawable.sit_ups),
            Exercise("Lunges", "Legs, glutes", R.drawable.lunges),
            Exercise("Bicep curls", "Biceps", R.drawable.bicep_curls),
            Exercise("Plank", "Abs, shoulders, back", R.drawable.plank),
            Exercise("Mountain climbers", "Core, shoulders, legs", R.drawable.mountain_climbers)
        )
        adapter.setData(exercises)


        // Initializare EditText
        searchEditText = findViewById(R.id.search_edit_text)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // nu trebuie să facem nimic înainte de modificare
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // apelăm metoda de căutare a adapterului cu textul de căutare introdus
                adapter.search(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                // nu trebuie să facem nimic după modificare
            }
        })

        // Solicitam actualizari pentru locatie
        createLocationRequest()
        startLocationUpdates()

        mapView.post {
            mapView.getMapAsync(this)
        }

        if (mediaController == null) {
            mediaController = MediaController(this)
            mediaController!!.setAnchorView(this.videoView)
        }

        videoView!!.setMediaController(mediaController)
        videoView!!.setVideoURI(Uri.parse("android.resource://"
                + packageName + "/" + R.raw.push_ups))
        videoView!!.requestFocus()
        videoView!!.start()

        // display a toast message if any
        // error occurs while playing the video
        videoView!!.setOnErrorListener { mp, what, extra ->
            Toast.makeText(applicationContext, "A aparut o eroare " +
                    "In timpul rularii video-ului !!!", Toast.LENGTH_LONG).show()
            false
        }
    }


    // Se deschide drawer-ul atunci cand iconita de meniu este apasata
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            drawerLayout.openDrawer(GravityCompat.START)
            return true
        }
        return false
    }



    private fun shareImage() {
        val drawable = photoImageView.drawable
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Image Description", null)
            val imageUri = Uri.parse(path)

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "image/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        } else {
            // Handle the case when the drawable is not a BitmapDrawable
            Toast.makeText(this, "No image to share", Toast.LENGTH_SHORT).show()
        }
    }


    // Inchide drawer-ul atunci cand este apasat back button
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun checkCameraPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }

    }

    private fun checkInternetPermission(){

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET), INTERNET_PERMISSION_REQUEST_CODE)
        }

    }


    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            takePictureLauncher.launch(takePictureIntent)
        }
    }

    private fun compressBitmap(bitmap: Bitmap?): Bitmap? {
        if (bitmap != null) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream) // Redimensionează și comprimă imaginea
            val compressedBitmap = BitmapFactory.decodeStream(ByteArrayInputStream(stream.toByteArray()))
            return compressedBitmap
        }
        return null
    }

    private val fitnessLocations = listOf(
        FitnessLocation("Stay Fit Gym - Cocor", "Strada Bărăției 31, București", LatLng(44.4302, 26.1043)),
        FitnessLocation("Stay Fit Gym - Liberty", "Strada Progresului 151-171, București", LatLng(44.4150, 26.0806)),
        FitnessLocation("Stay Fit Gym - Nicolae Titulescu", "Șoseaua Nicolae Titulescu 171, București", LatLng(44.4509, 26.0708)),
        FitnessLocation("Stay Fit Gym - Domenii", "Bulevardul Ion Mihalache 128 – 130, București", LatLng(44.4679, 26.0675)),
        FitnessLocation("Stay Fit Gym - Teiul Doamnei", "Strada Teiul Doamnei 35A, București", LatLng(44.4645, 26.1262)),
        FitnessLocation("Stay Fit Gym - Fizicienilor", "Strada Fizicienilor 1, București", LatLng(44.4173, 26.1505)),
        FitnessLocation("Stay Fit Gym - Pipera", "Bulevardul Dimitrie Pompeiu 9-9A, București", LatLng(44.4865, 26.1203)),
        FitnessLocation("Stay Fit Gym - Pantelimon", "Bulevardul Chișinău 1, București", LatLng(44.4475, 26.1574)),
        FitnessLocation("Stay Fit Gym - Vitan", "Strada Răcari 27, București", LatLng(44.4157, 26.1369)),
        FitnessLocation("Stay Fit Gym - Colosseum", "Șoseaua Chitilei 284, București", LatLng(44.5002, 26.0180)),
        FitnessLocation("Stay Fit Gym - Lemon Park", "Strada Popasului, Voluntari", LatLng(44.4983, 26.1519)),
        FitnessLocation("Stay Fit Gym - Grand Arena", "Bulevardul Metalurgiei 12-18, București", LatLng(44.3791, 26.1190))
    )


    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isZoomGesturesEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true
        googleMap.uiSettings.isScrollGesturesEnabled = true
        googleMap.uiSettings.isCompassEnabled = false

        // Permisiunea de locație este necesară pentru a afișa harta
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Activează butonul pentru localizare si creeaza marker-ele
            googleMap.isMyLocationEnabled = true
            createMarkers()
        } else {
            // Dacă permisiunea nu a fost acordată, solicită-o utilizatorului
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

    }

    private fun createMarkers() {

        for (location in fitnessLocations) {
            val markerOptions = MarkerOptions()
                .position(location.latLng)
                .title(location.name)
                .snippet(location.address)
            googleMap.addMarker(markerOptions)
        }

        // Adaug marker pentru locatia curenta
        val currentLocation = LatLng(latitude, longitude)
        googleMap.addMarker(MarkerOptions().position(currentLocation).title("Locația curenta"))
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 14f))
        currentLocationMarker = googleMap.addMarker(MarkerOptions().position(currentLocation).title("Locația curentă"))

    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private fun createLocationRequest() {
        mLocationRequest = LocationRequest.create()
        mLocationRequest?.interval = UPDATE_INTERVAL
        mLocationRequest?.fastestInterval = FASTEST_INTERVAL
        mLocationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private var isMapCentered = false

    private fun startLocationUpdates() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permission if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fun updateCurrentLocationMarker(googleMap: GoogleMap, currentLocation: LatLng) {
            currentLocationMarker?.remove()
            currentLocationMarker = googleMap.addMarker(MarkerOptions().position(currentLocation).title("Locatia curenta"))
        }

        fusedLocationClient.requestLocationUpdates(mLocationRequest!!, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location: Location? = locationResult.lastLocation
                if (location != null && !isMapCentered) {
                    latitude = location.latitude
                    longitude = location.longitude

                    // Update the marker on the map
                    val currentLocation = LatLng(latitude, longitude)
                    updateCurrentLocationMarker(googleMap, currentLocation)
                    //googleMap.clear()
                    //googleMap.addMarker(
                    //    MarkerOptions().position(currentLocation).title("Locatia curenta")


                    // Move the camera to the current location
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 14f))

                    isMapCentered = true

                    //googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 14f))
                    //googleMap.animateCamera(CameraUpdateFactory.zoomTo(14f))

                }
            }
        }, null)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val REQUEST_CAMERA_PERMISSION = 1002
        private const val INTERNET_PERMISSION_REQUEST_CODE = 1003
    }

}

class ExerciseAdapter : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    private var exercises = emptyList<Exercise>()
    private var filteredExercises = emptyList<Exercise>()

    inner class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.title_text_view)
        private val musclesTextView: TextView = itemView.findViewById(R.id.muscles_text_view)
        private val imageView: ImageView = itemView.findViewById(R.id.image_view)

        fun bind(exercise: Exercise) {
            titleTextView.text = exercise.title
            musclesTextView.text = exercise.muscles
            imageView.setImageResource(exercise.image)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_exercise, parent, false)
        return ExerciseViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(filteredExercises[position])
    }

    override fun getItemCount() = filteredExercises.size

    fun setData(exercises: List<Exercise>) {
        this.exercises = exercises
        filterExercises("")
    }

    fun search(text: String) {
        filterExercises(text)
    }


    private fun filterExercises(text: String) {
        filteredExercises = exercises.filter {
            it.title.contains(text, true) || it.muscles.contains(text, true)
        }
        notifyDataSetChanged()
    }
}

data class Exercise(val title: String, val muscles: String, val image: Int)

data class FitnessLocation(val name: String, val address: String, val latLng: LatLng)
