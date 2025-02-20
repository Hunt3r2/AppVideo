package com.example.appvideo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : ComponentActivity() {

    private val PICK_VIDEO_REQUEST = 1
    private lateinit var videoView: VideoView
    private lateinit var botonPlay: ImageButton
    private var isPlaying = false
    private lateinit var duracion: TextView
    private lateinit var barra: SeekBar
    private lateinit var anterior: ImageButton
    private lateinit var siguiente: ImageButton
    private lateinit var cargarVideo: Button
    private val handler = Handler(Looper.getMainLooper())
    private var videoActual = 0
    private val videos = mutableListOf<VideoItem>() // Lista para almacenar URI y nombres de videos
    private lateinit var recyclerView: RecyclerView


    data class VideoItem(val uri: Uri, val name: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar UI
        videoView = findViewById(R.id.videoView)
        botonPlay = findViewById(R.id.botonPlay)
        duracion = findViewById(R.id.duracion)
        barra = findViewById(R.id.seekBar)
        anterior = findViewById(R.id.anterior)
        siguiente = findViewById(R.id.siguiente)
        cargarVideo = findViewById(R.id.cargarVideo)
        recyclerView = findViewById(R.id.recyclerView)

        if (videoView == null || botonPlay == null || duracion == null || duracion == null) {
            Log.e("MainActivity", "Algunos elementos no fueron encontrados.")
        }


        val videoAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = layoutInflater.inflate(R.layout.item_video, parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val videoItem = videos[position]

                val textView = holder.itemView.findViewById<TextView>(R.id.videoName)
                textView.text = videoItem.name
                holder.itemView.setOnClickListener {
                    reproducirVideoSeleccionado(videoItem.uri)
                }
            }

            override fun getItemCount(): Int = videos.size
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = videoAdapter

        cargarListaDeVideosGuardados()
        checkPermissions()

        if (videos.isNotEmpty()) {
            reproducirVideoSeleccionado(videos[videoActual].uri)
        }

        botonPlay.setOnClickListener {
            if (isPlaying) {
                pausarVideo()
            } else {
                reproducirVideo()
            }
        }

        siguiente.setOnClickListener {
            siguienteVideo()
        }

        anterior.setOnClickListener {
            anteriorVideo()
        }

        barra.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    videoView.seekTo(progress)
                    actualizarTiempo()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Botón cargar video
        cargarVideo.setOnClickListener {
            cargarVideoDesdeAlmacenamiento()
        }

        handler.postDelayed(updateProgressTask, 1000)
    }

    private fun reproducirVideoSeleccionado(videoUri: Uri?) {
        if (videoUri == null) {
            // Si no hay video seleccionado, usar el video por defecto de la carpeta raw
            val videoId = R.raw.video1 // Nombre del archivo sin la extensión
            val uri = Uri.parse("android.resource://${packageName}/$videoId")
            videoView.setVideoURI(uri)
            // Mostrar el nombre del video por defecto
            val videoName = "Video por defecto" // Puedes asignar el nombre que desees
            val textView = findViewById<TextView>(R.id.videoName)
            textView.text = videoName
        } else {
            // Reproducir el video seleccionado
            videoView.setVideoURI(videoUri)
            // Mostrar el nombre del video seleccionado
            val videoName = videoUri.lastPathSegment ?: "Video desconocido"
            val textView = findViewById<TextView>(R.id.videoName)
            textView.text = videoName // Actualizamos el nombre del video en la interfaz
        }

        videoView.setOnPreparedListener {
            barra.max = videoView.duration
            actualizarTiempo()
            reproducirVideo() // Solo se debe llamar después de la preparación del video
        }
    }


    private fun cargarVideoDesdeAlmacenamiento() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        intent.type = "video/*"
        startActivityForResult(intent, PICK_VIDEO_REQUEST)
    }

    private fun guardarListaDeVideos(videoUri: Uri, videoName: String) {
        val sharedPreferences = getSharedPreferences("VideoPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val videoList = sharedPreferences.getStringSet("videoList", mutableSetOf()) ?: mutableSetOf()
        videoList.add(videoUri.toString())

        editor.putStringSet("videoList", videoList)
        editor.apply()
    }

    private fun cargarListaDeVideosGuardados() {
        val sharedPreferences = getSharedPreferences("VideoPrefs", MODE_PRIVATE)
        val videoUris = sharedPreferences.getStringSet("videoList", mutableSetOf()) ?: mutableSetOf()
        videos.clear()
        for (uriStr in videoUris) {
            val uri = Uri.parse(uriStr)
            val videoName = uri.lastPathSegment ?: "Video desconocido"
            videos.add(VideoItem(uri, videoName)) // Almacena el URI y nombre del video
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_VIDEO_REQUEST && resultCode == RESULT_OK && data != null) {
            val selectedVideoUri: Uri? = data.data
            selectedVideoUri?.let {
                val videoName = it.lastPathSegment ?: "Nuevo video"
                guardarListaDeVideos(it, videoName)
                videos.add(VideoItem(it, videoName))
                recyclerView.adapter?.notifyDataSetChanged() // Notificar al adapter
                videoActual = videos.size - 1 // Reproducir el nuevo video agregado
                reproducirVideoSeleccionado(it)
            }
        }
    }

    private fun reproducirVideo() {
        videoView.start()
        botonPlay.setImageResource(android.R.drawable.ic_media_pause)
        isPlaying = true
    }

    private fun pausarVideo() {
        videoView.pause()
        botonPlay.setImageResource(android.R.drawable.ic_media_play)
        isPlaying = false
    }

    private fun siguienteVideo() {
        if (videos.isNotEmpty()) {
            videoActual = if (videoActual < videos.size - 1) videoActual + 1 else 0
            reproducirVideoSeleccionado(videos[videoActual].uri)
        } else {
            Toast.makeText(this, "No hay videos para reproducir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun anteriorVideo() {
        if (videos.isNotEmpty()) {
            videoActual = if (videoActual > 0) videoActual - 1 else videos.size - 1
            reproducirVideoSeleccionado(videos[videoActual].uri)
        } else {
            Toast.makeText(this, "No hay videos para reproducir", Toast.LENGTH_SHORT).show()
        }
    }

    private val updateProgressTask = object : Runnable {
        override fun run() {
            if (videoView.isPlaying) {
                barra.progress = videoView.currentPosition
                actualizarTiempo()
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Realiza las modificaciones necesarias para el landscape
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Realiza las modificaciones necesarias para el portrait
        }
    }

    private fun actualizarTiempo() {
        val currentTime = tiempo(videoView.currentPosition)
        val totalTime = tiempo(videoView.duration)
        duracion.text = "$currentTime / $totalTime"
    }

    private fun tiempo(milisec: Int): String {
        val seconds = (milisec / 1000) % 60
        val minutes = (milisec / (1000 * 60)) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            cargarVideoDesdeAlmacenamiento()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            cargarVideoDesdeAlmacenamiento()
        } else {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // Variables para controlar la visibilidad de los botones
    private val fadeOutHandler = Handler(Looper.getMainLooper())
    private val fadeOutRunnable = object : Runnable {
        override fun run() {
            // Desvanecer los botones y la barra
            botonPlay.animate().alpha(0f).setDuration(500).start()
            duracion.animate().alpha(0f).setDuration(500).start()
            barra.animate().alpha(0f).setDuration(500).start()
            anterior.animate().alpha(0f).setDuration(500).start()
            siguiente.animate().alpha(0f).setDuration(500).start()
            cargarVideo.animate().alpha(0f).setDuration(500).start()
        }
    }

    private fun setupTouchListener() {
        // Volver a mostrar los botones al tocar la pantalla
        findViewById<LinearLayout>(R.id.botonesLayout).setOnTouchListener { _, _ ->
            // Hacer que los botones y la barra vuelvan a ser visibles
            botonPlay.animate().alpha(1f).setDuration(500).start()
            duracion.animate().alpha(1f).setDuration(500).start()
            barra.animate().alpha(1f).setDuration(500).start()
            anterior.animate().alpha(1f).setDuration(500).start()
            siguiente.animate().alpha(1f).setDuration(500).start()
            cargarVideo.animate().alpha(1f).setDuration(500).start()

            // Volver a iniciar el fade out después de 3 segundos
            fadeOutHandler.removeCallbacks(fadeOutRunnable)
            fadeOutHandler.postDelayed(fadeOutRunnable, 3000)

            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressTask)
        videoView.stopPlayback()
    }
}
