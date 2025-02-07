import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.appvideo.R

class VideoAdapter(private val videoList: List<Uri>, private val onClick: (Uri) -> Unit) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val videoUri = videoList[position]

        // Obtener el nombre del archivo
        val fileName = getFileNameFromUri(videoUri)

        // Establecer el nombre en el TextView del item
        holder.videoName.text = fileName
    }


    override fun getItemCount(): Int {
        return videoList.size
    }

    // ViewHolder para manejar los elementos de la lista
    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val videoName: TextView = itemView.findViewById(R.id.videoName)
    }

    // Función para obtener el nombre del archivo a partir de la URI
    private fun getFileNameFromUri(uri: Uri): String {
        val filePath = uri.path ?: return "Desconocido"
        val fileName = filePath.substring(filePath.lastIndexOf("/") + 1)
        return fileName
    }
}
