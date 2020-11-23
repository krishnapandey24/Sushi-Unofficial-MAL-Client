package com.destructo.sushi.ui.manga

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.destructo.sushi.databinding.ListItemMangaBinding
import com.destructo.sushi.model.mal.mangaRanking.MangaRankingData
import com.destructo.sushi.ui.listener.ListEndListener
import com.destructo.sushi.ui.manga.mangaDetails.MangaDetailListener

class MangaAdapter(private val mangaDetailListener: MangaDetailListener) :
    ListAdapter<MangaRankingData, MangaAdapter.ViewHolder>(MangaDiffUtil()) {

    private var listEndListener: ListEndListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaAdapter.ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mangaEntity = getItem(position)
        holder.bind(mangaEntity, mangaDetailListener)
        if (position == currentList.size - 2) run {
            listEndListener?.onEndReached(position)
        }
    }

    fun setListEndListener(listEndListener: ListEndListener) {
        this.listEndListener = listEndListener
    }

    class ViewHolder(val binding: ListItemMangaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mangaEntity: MangaRankingData, mangaDetailListener: MangaDetailListener) {
            binding.mangaEntity = mangaEntity.manga
            binding.mangaListener = mangaDetailListener
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemMangaBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(
                    binding
                )
            }
        }
    }


}

class MangaDiffUtil : DiffUtil.ItemCallback<MangaRankingData>() {
    override fun areItemsTheSame(oldItem: MangaRankingData, newItem: MangaRankingData): Boolean {
        return oldItem.manga?.id == newItem.manga?.id

    }

    override fun areContentsTheSame(oldItem: MangaRankingData, newItem: MangaRankingData): Boolean {
        return oldItem.manga?.id == newItem.manga?.id
                && oldItem.manga?.title == newItem.manga?.title
    }

}