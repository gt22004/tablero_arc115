package com.example.espdisplay.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.espdisplay.R
import com.example.espdisplay.models.ESPConfig
import com.example.espdisplay.models.GroupImage
import coil.request.CachePolicy


class GroupImagesAdapter(
    private val espConfig: ESPConfig,
    private val onImageClick: (GroupImage) -> Unit
) : ListAdapter<GroupImage, GroupImagesAdapter.ImageViewHolder>(ImageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val image = getItem(position)
        holder.bind(image)
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)

        fun bind(image: GroupImage) {
            val timestamp = System.currentTimeMillis()
            val imageUrl = "http://${espConfig.ipAddress}:${espConfig.port}${image.imageUrl}?t=$timestamp"

            android.util.Log.d("ImageAdapter", "Cargando imagen: $imageUrl")

            imageView.load(imageUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_dialog_alert)

                memoryCachePolicy(CachePolicy.DISABLED)
                diskCachePolicy(CachePolicy.DISABLED)

                listener(
                    onSuccess = { _, _ ->
                        android.util.Log.d("ImageAdapter", "✓ Imagen cargada: $imageUrl")
                    },
                    onError = { _, result ->
                        android.util.Log.e("ImageAdapter", "✗ Error cargando: $imageUrl - ${result.throwable?.message}")
                    }
                )
            }

            imageView.setOnClickListener {
                onImageClick(image)
            }
        }
    }
    class ImageDiffCallback : DiffUtil.ItemCallback<GroupImage>() {
        override fun areItemsTheSame(oldItem: GroupImage, newItem: GroupImage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GroupImage, newItem: GroupImage): Boolean {
            return oldItem == newItem
        }
    }
}