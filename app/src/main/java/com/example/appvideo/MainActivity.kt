package com.example.appvideo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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

    private val video_seleccionado = 1
    private lateinit var videoView: VideoView
    private lateinit var botonPlay: ImageButton
    private var estaPuesto = false
    private lateinit var duracion: TextView
    private lateinit var barra: SeekBar
    private lateinit var anterior: ImageButton
    private lateinit var siguiente: ImageButton
    private lateinit var cargarVideo: Button
    private val handler = Handler(Looper.getMainLooper())
    private var videoActual = 0
    private val videos = mutableListOf<VideoItem>()
    private lateinit var recyclerView: RecyclerView


    data class VideoItem(val uri: Uri, val name: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            if (estaPuesto) {
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

        cargarVideo.setOnClickListener {
            cargarVideoDesdeAlmacenamiento()
        }

        handler.postDelayed(updateProgressTask, 1000)
    }

    private fun reproducirVideoSeleccionado(videoUri: Uri?) {
        if (videoUri == null) {
            val videoId = R.raw.video1
            val uri = Uri.parse("android.resource://${packageName}/$videoId")
            videoView.setVideoURI(uri)
            val videoName = "Video por defecto"
            val textView = findViewById<TextView>(R.id.videoName)
            textView.text = videoName
        } else {
            //reproducir el video seleccionado
            videoView.setVideoURI(videoUri)
            //mostrar el nombre del video seleccionado
            val videoName = videoUri.lastPathSegment ?: "Video desconocido"
            val textView = findViewById<TextView>(R.id.videoName)
            textView.text = videoName
        }

        videoView.setOnPreparedListener {
            barra.max = videoView.duration
            actualizarTiempo()
            reproducirVideo()
        }
    }


    private fun cargarVideoDesdeAlmacenamiento() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        intent.type = "video/*"
        startActivityForResult(intent, video_seleccionado)
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
            videos.add(VideoItem(uri, videoName))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == video_seleccionado && resultCode == RESULT_OK && data != null) {
            val selectedVideoUri: Uri? = data.data
            selectedVideoUri?.let {
                val videoName = it.lastPathSegment ?: "Nuevo video"
                guardarListaDeVideos(it, videoName)
                videos.add(VideoItem(it, videoName))
                recyclerView.adapter?.notifyDataSetChanged()
                videoActual = videos.size - 1
                reproducirVideoSeleccionado(it)
            }
        }
    }

    private fun reproducirVideo() {
        videoView.start()
        botonPlay.setImageResource(android.R.drawable.ic_media_pause)
        estaPuesto = true
    }

    private fun pausarVideo() {
        videoView.pause()
        botonPlay.setImageResource(android.R.drawable.ic_media_play)
        estaPuesto = false
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("videoPosition", videoView.currentPosition)
        outState.putBoolean("estaPuesto", estaPuesto)
        outState.putInt("videoActual", videoActual)
        outState.putString("videoUri", videos[videoActual].uri.toString())
    }


    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val videoPosition = savedInstanceState.getInt("videoPosition", 0)
        estaPuesto = savedInstanceState.getBoolean("estaPuesto", false)
        videoActual = savedInstanceState.getInt("videoActual", 0)
        val videoUriString = savedInstanceState.getString("videoUri")

        if (videoUriString != null) {
            val videoUri = Uri.parse(videoUriString)
            reproducirVideoSeleccionado(videoUri)
            if (videoPosition > 0) {
                videoView.seekTo(videoPosition)
            }
        }

        if (estaPuesto) {
            videoView.start()
            botonPlay.setImageResource(android.R.drawable.ic_media_pause)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressTask)
        videoView.stopPlayback()
    }
}
