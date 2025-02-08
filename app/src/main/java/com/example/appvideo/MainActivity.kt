package com.example.appvideo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
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
    private var videos = mutableListOf<Uri>()
    private lateinit var recyclerView: RecyclerView

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            cargarVideoDesdeAlmacenamiento()
        } else {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickVideoResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedVideoUri: Uri? = result.data?.data
            selectedVideoUri?.let {
                guardarListaDeVideos(it)
                videos.add(it)
                recyclerView.adapter?.notifyDataSetChanged()
                videoActual = videos.size - 1
                reproducirVideoSeleccionado(it)
            } ?: run {
                Toast.makeText(this, "No se seleccionó un video", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

        val videoAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = layoutInflater.inflate(R.layout.item_video, parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val videoUri = videos[position]
                val videoName = videoUri.lastPathSegment // Obtener el nombre del archivo

                val textView = holder.itemView.findViewById<TextView>(R.id.videoName)
                textView.text = videoName
                holder.itemView.setOnClickListener {
                    reproducirVideoSeleccionado(videoUri)
                }
            }

            override fun getItemCount(): Int = videos.size
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = videoAdapter

        cargarListaDeVideosGuardados()
        checkPermissions()

        if (videos.isNotEmpty() && videoActual < videos.size) {
            reproducirVideoSeleccionado(videos[videoActual])
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
            val videoName = videoUri.lastPathSegment
            val textView = findViewById<TextView>(R.id.videoName)
            textView.text = videoName
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
        pickVideoResultLauncher.launch(intent)
    }

    private fun guardarListaDeVideos(videoUri: Uri) {
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
        videos = videoUris.map { Uri.parse(it) }.toMutableList() // Convierte los Strings en Uris
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
            reproducirVideoSeleccionado(videos[videoActual])
        } else {
            // Si la lista de videos está vacía, no hacer nada o mostrar un mensaje
            Toast.makeText(this, "No hay videos para reproducir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun anteriorVideo() {
        if (videos.isNotEmpty()) {
            videoActual = if (videoActual > 0) videoActual - 1 else videos.size - 1
            reproducirVideoSeleccionado(videos[videoActual])
        } else {
            // Si la lista de videos está vacía, no hacer nada o mostrar un mensaje
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressTask)
        videoView.stopPlayback()
    }
}
