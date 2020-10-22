package com.destructo.sushi.ui.anime.adapter

import com.destructo.sushi.model.jikan.character.Character

class AnimeCharacterListener(val clickListener: (characterMalId: Int?) -> Unit) {
    fun onClick(malId: Int) = clickListener(malId)
}