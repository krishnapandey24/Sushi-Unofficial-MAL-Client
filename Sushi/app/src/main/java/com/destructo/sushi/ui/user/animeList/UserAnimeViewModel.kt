package com.destructo.sushi.ui.user.animeList

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.destructo.sushi.enum.UserAnimeListSort
import com.destructo.sushi.model.mal.updateUserAnimeList.UpdateUserAnime
import com.destructo.sushi.model.mal.userAnimeList.UserAnimeList
import com.destructo.sushi.network.Resource
import com.destructo.sushi.room.AnimeDetailsDao
import com.destructo.sushi.room.UserAnimeDao
import com.destructo.sushi.util.Event
import kotlinx.coroutines.launch
import timber.log.Timber

class UserAnimeViewModel
@ViewModelInject
constructor(
    @Assisted
    savedStateHandle: SavedStateHandle,
    private val myAnimeListRepo: MyAnimeListRepository,
    private val userAnimeListDao: UserAnimeDao,
    private val animeDetailsDao: AnimeDetailsDao
) : ViewModel() {

    val userAnimeListState: LiveData<Resource<UserAnimeList>> = myAnimeListRepo.userAnimeList

    val userAnimeStatus: LiveData<Resource<UpdateUserAnime>> = myAnimeListRepo.userAnimeStatus

    var userAnimeList = userAnimeListDao.getUserAnimeList()

    var userSortType = MutableLiveData(Event(UserAnimeListSort.BY_TITLE.value))

    val nextPage = myAnimeListRepo.nextPage

    fun addEpisodeAnime(animeId:String,numberOfEp:Int?, status:String?){
        viewModelScope.launch { myAnimeListRepo.addEpisode(animeId, numberOfEp, status) }
    }

    fun getUserAnimeList(sortType : String){
        Timber.e("GetUserAnime")
        viewModelScope.launch { myAnimeListRepo.getUserAnimeList(sortType) }
    }

    fun getNextPage(){
        Timber.e("GetNextAnime")
        viewModelScope.launch {myAnimeListRepo.getNextPage() }
    }

    fun setSortType(sort_by:String){
        userSortType.value = Event(sort_by)
    }

    fun clearList(){
        viewModelScope.launch{userAnimeListDao.clear()}
    }

    fun clearAnimeDetails(animeId:Int){
        viewModelScope.launch{animeDetailsDao.deleteAnimeDetailById(animeId)}
    }

}

