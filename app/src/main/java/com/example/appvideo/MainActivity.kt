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

        // Inicializar RecyclerView
        val videoAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            // Crear una vista para cada ítem
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val view = layoutInflater.inflate(R.layout.item_video, parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }

            // Establecer los datos en la vista
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val videoUri = videos[position]
                val videoName = videoUri.lastPathSegment // Obtener el nombre del archivo

                val textView = holder.itemView.findViewById<TextView>(R.id.videoName)
                textView.text = videoName
                holder.itemView.setOnClickListener {
                    reproducirVideoSeleccionado(videoUri)
                }
            }

            // Contar el número de elementos en la lista
            override fun getItemCount(): Int = videos.size
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = videoAdapter

        cargarListaDeVideosGuardados()
        checkPermissions()

        if (videos.isNotEmpty()) {
            reproducirVideoSeleccionado(videos[videoActual])
        }

        // Botón play/pause
        botonPlay.setOnClickListener {
            if (isPlaying) {
                pausarVideo()
            } else {
                reproducirVideo()
            }
        }

        // Botón siguiente
        siguiente.setOnClickListener {
            siguienteVideo()
        }

        // Botón anterior
        anterior.setOnClickListener {
            anteriorVideo()
        }

        // Control del SeekBar
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

    private fun reproducirVideoSeleccionado(videoUri: Uri) {
        videoView.setVideoURI(videoUri)
        videoView.setOnPreparedListener {
            barra.max = videoView.duration
            actualizarTiempo()
            reproducirVideo()
        }
    }

    private fun cargarVideoDesdeAlmacenamiento() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        intent.type = "video/*"
        startActivityForResult(intent, PICK_VIDEO_REQUEST)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_VIDEO_REQUEST && resultCode == RESULT_OK && data != null) {
            val selectedVideoUri: Uri? = data.data
            selectedVideoUri?.let {
                guardarListaDeVideos(it)
                videos.add(it)
                recyclerView.adapter?.notifyDataSetChanged() // Notificar al adapter
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
        videoActual = if (videoActual < videos.size - 1) videoActual + 1 else 0
        reproducirVideoSeleccionado(videos[videoActual])
    }

    private fun anteriorVideo() {
        videoActual = if (videoActual > 0) videoActual - 1 else videos.size - 1
        reproducirVideoSeleccionado(videos[videoActual])
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

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            cargarVideoDesdeAlmacenamiento()
        } else {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressTask)
        videoView.stopPlayback()
    }
}
