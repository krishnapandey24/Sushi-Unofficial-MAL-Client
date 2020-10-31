package com.destructo.sushi.ui.user.animeList

import com.destructo.sushi.model.mal.anime.Anime

class AddEpisodeListener(val clickListener: (anime : Anime?) -> Unit) {
    fun onClick(anime:Anime?) = clickListener(anime)
}