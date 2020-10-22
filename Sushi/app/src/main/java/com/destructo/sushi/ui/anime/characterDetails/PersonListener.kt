package com.destructo.sushi.ui.anime.characterDetails

import com.destructo.sushi.model.jikan.character.PersonEntity

class PersonListener(val clickListener: (malId: Int?) -> Unit) {
    fun onClick(person: PersonEntity) = clickListener(person.malId)
}