package com.destructo.sushi.ui.anime.animeDetails

import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import com.destructo.sushi.ALL_ANIME_FIELDS
import com.destructo.sushi.CACHE_EXPIRE_TIME_LIMIT
import com.destructo.sushi.DEFAULT_USER_LIST_PAGE_LIMIT
import com.destructo.sushi.DETAILS_CACHE_EXPIRE_TIME_LIMIT
import com.destructo.sushi.model.database.AnimeCharacterListEntity
import com.destructo.sushi.model.database.AnimeDetailEntity
import com.destructo.sushi.model.database.AnimeReviewsEntity
import com.destructo.sushi.model.database.AnimeVideosEntity
import com.destructo.sushi.model.jikan.anime.core.AnimeCharacterAndStaff
import com.destructo.sushi.model.jikan.anime.core.AnimeReviews
import com.destructo.sushi.model.jikan.anime.core.AnimeVideo
import com.destructo.sushi.model.mal.anime.Anime
import com.destructo.sushi.model.mal.updateUserAnimeList.UpdateUserAnime
import com.destructo.sushi.model.mal.userAnimeList.UserAnimeList
import com.destructo.sushi.network.JikanApi
import com.destructo.sushi.network.MalApi
import com.destructo.sushi.network.Resource
import com.destructo.sushi.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class AnimeDetailRepository
@Inject
constructor(
    private val malApi: MalApi,
    private val jikanApi: JikanApi,
    private val animeDetailsDao: AnimeDetailsDao,
    private val animeCharacterListDao: AnimeCharacterListDao,
    private val animeVideosDao: AnimeVideoListDao,
    private val animeReviewsDao: AnimeReviewListDao,
    private val userAnimeListDao: UserAnimeListDao

) {

    var animeDetail: MutableLiveData<Resource<Anime>> = MutableLiveData()

    var animeCharacterAndStaff: MutableLiveData<Resource<AnimeCharacterAndStaff>> =
        MutableLiveData()

    var animeVideosAndEpisodes: MutableLiveData<Resource<AnimeVideo>> = MutableLiveData()

    var animeReview: MutableLiveData<Resource<AnimeReviews>> = MutableLiveData()

    var userAnimeStatus: MutableLiveData<Resource<UpdateUserAnime>> = MutableLiveData()

    var userAnimeRemove: MutableLiveData<Resource<Unit>> = MutableLiveData()

    fun getAnimeDetail(malId: Int, isEdited: Boolean) {
        animeDetail.value = Resource.loading(null)

        GlobalScope.launch {
            val animeDetailCache = animeDetailsDao.getAnimeDetailsById(malId)

            if (animeDetailCache != null){

                    if((System.currentTimeMillis() - animeDetailCache.time) > DETAILS_CACHE_EXPIRE_TIME_LIMIT
                        || isEdited) {
                        animeDetailCall(malId)
                    }else{
                        val mAnime = animeDetailCache.anime
                        withContext(Dispatchers.Main) {
                        animeDetail.value = Resource.success(mAnime)
                        }
                    }
            }else{
                animeDetailCall(malId)
            }
        }
    }

    fun getAnimeCharacters(malId: Int) {
        animeCharacterAndStaff.value = Resource.loading(null)
        GlobalScope.launch {
            val animeCharacterListCache = animeCharacterListDao.getAnimeCharactersById(malId)

            if (animeCharacterListCache != null){

                if((System.currentTimeMillis() - animeCharacterListCache.time) > CACHE_EXPIRE_TIME_LIMIT) {
                    animeCharacterCall(malId)
                }else{
                    val mAnime = animeCharacterListCache.characterAndStaffList
                    withContext(Dispatchers.Main) {
                        animeCharacterAndStaff.value = Resource.success(mAnime)
                    }
                }
            }else{
                animeCharacterCall(malId)
            }

        }
    }


    fun getAnimeVideos(malId: Int) {
        animeVideosAndEpisodes.value = Resource.loading(null)
        GlobalScope.launch {
            val animeVideosListCache = animeVideosDao.getAnimeVideosById(malId)

            if (animeVideosListCache != null){

                if((System.currentTimeMillis() - animeVideosListCache.time) > CACHE_EXPIRE_TIME_LIMIT) {
                    animeVideoCall(malId)
                }else{
                    val mAnime = animeVideosListCache.videosAndEpisodes
                    withContext(Dispatchers.Main) {
                        animeVideosAndEpisodes.value = Resource.success(mAnime)
                    }
                }
            }else{
                animeVideoCall(malId)
            }
        }
    }


    fun getAnimeReviews(malId: Int, page: String) {
        animeReview.value = Resource.loading(null)
        GlobalScope.launch {
            val animeReviewListCache = animeReviewsDao.getAnimeReviewsById(malId)

            if (animeReviewListCache != null){

                if((System.currentTimeMillis() - animeReviewListCache.time) > CACHE_EXPIRE_TIME_LIMIT) {
                    animeReviewCall(malId, page)
                }else{
                    val mAnime = animeReviewListCache.reviewList
                    withContext(Dispatchers.Main) {
                        animeReview.value = Resource.success(mAnime)
                    }
                }
            }else{
                animeReviewCall(malId, page)
            }
        }
    }


    fun updateAnimeUserList(animeId:String, status:String?=null,
                            is_rewatching:Boolean?=null, score:Int?=null,
                            num_watched_episodes:Int?=null, priority:Int?=null,
                            num_times_rewatched:Int?=null, rewatch_value:Int?=null,
                            tags:List<String>?=null, comments:String?=null) {
        userAnimeStatus.value = Resource.loading(null)

        GlobalScope.launch {
            val addEpisodeDeferred = malApi.updateUserAnime(animeId,
                status,is_rewatching,score,num_watched_episodes,
                priority,num_times_rewatched,rewatch_value,tags,comments)
            try {
                val animeStatus = addEpisodeDeferred.await()
                updateUserAnimeList(animeId.toInt())
                withContext(Dispatchers.Main){
                    userAnimeStatus.value = Resource.success(animeStatus)
                }
            }catch (e: java.lang.Exception){
                withContext(Dispatchers.Main){
                    userAnimeStatus.value = Resource.error(e.message ?: "", null)
                }
            }
        }
    }


    fun removeAnimeFromList(animeId: String){

        GlobalScope.launch {
            try {
               malApi.deleteAnimeFromList(animeId).await()
                withContext(Dispatchers.Main){
                    userAnimeRemove.value = Resource.success(Unit)
                }
            }catch (e: java.lang.Exception){
                withContext(Dispatchers.Main){
                    userAnimeRemove.value = Resource.error(e.message ?: "", null)
                    Timber.e("Error: %s",e.message)
                }
            }
        }

    }

    suspend fun animeDetailCall(malId:Int){

        val animeId: String = malId.toString()
        val getAnimeByIdDeferred = malApi.getAnimeByIdAsync(animeId, ALL_ANIME_FIELDS)
        try {
            val animeDetails = getAnimeByIdDeferred.await()
            val animeRequest = AnimeDetailEntity(
                anime = animeDetails,
                id = animeDetails.id!!,
                time = System.currentTimeMillis()
            )
            animeDetailsDao.insertAnimeDetails(animeRequest)
            withContext(Dispatchers.Main) {
                animeDetail.value = Resource.success(animeRequest.anime)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                animeDetail.value = Resource.error(e.message ?: "", null)
            }
        }
    }

    private suspend fun animeCharacterCall(malId:Int) {

        val animeId: String = malId.toString()
        val getAnimeCharactersDeferred = jikanApi.getCharacterAndStaffAsync(animeId)
        try {
            val animeCharactersAndStaffList = getAnimeCharactersDeferred.await()
            val animeCharacterListEntity = AnimeCharacterListEntity(
                characterAndStaffList = animeCharactersAndStaffList,
                time = System.currentTimeMillis(),
                id = malId
            )
            animeCharacterListDao.insertAnimeCharacters(animeCharacterListEntity)
            withContext(Dispatchers.Main) {
                animeCharacterAndStaff
                    .value = Resource.success(animeCharacterListEntity.characterAndStaffList)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                animeCharacterAndStaff.value = Resource.error(e.message ?: "", null)
            }
        }
    }

    private suspend fun animeVideoCall(malId:Int) {
        val animeId: String = malId.toString()
        val getAnimeVideosDeferred = jikanApi.getAnimeVideosAsync(animeId)
        try {
            val animeVideosList = getAnimeVideosDeferred.await()
            val animeVideoListEntity = AnimeVideosEntity(
                videosAndEpisodes = animeVideosList,
                time = System.currentTimeMillis(),
                id = malId,

            )
            animeVideosDao.insertAnimeVideos(animeVideoListEntity)
            withContext(Dispatchers.Main) {
                animeVideosAndEpisodes
                    .value = Resource.success(animeVideoListEntity.videosAndEpisodes)
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                animeVideosAndEpisodes.value = Resource.error(e.message ?: "", null)
            }
        }
    }

    suspend fun animeReviewCall(malId:Int, page:String) {
        val animeId: String = malId.toString()
        val getAnimeReviewsDeferred = jikanApi.getAnimeReviewsAsync(animeId, page)
        try {
            val animeReviews = getAnimeReviewsDeferred.await()
            val animeReviewListEntity = AnimeReviewsEntity(
                reviewList = animeReviews,
                time = System.currentTimeMillis(),
                id = malId,
                currentPage = page
            )
            animeReviewsDao.insertAnimeReviews(animeReviewListEntity)
            withContext(Dispatchers.Main) {
                animeReview.value = Resource.success(animeReviewListEntity.reviewList)
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                animeReview.value = Resource.error(e.message ?: "", null)
            }
        }
    }

    private fun updateUserAnimeList(animeId: Int){
        val anime = userAnimeListDao.getUserAnimeById(animeId)
        anime.status?.let {
            loadPage(it, anime.offset, animeId.toInt())
        }
    }

    private fun loadPage(
        animeStatus: String,
        offset:String?,
        animeId:Int
    ) {
        if(!offset.isNullOrBlank()){
            GlobalScope.launch {
                val getUserAnimeDeferred = malApi.getUserAnimeListAsync(
                    "@me", DEFAULT_USER_LIST_PAGE_LIMIT,
                    animeStatus, null, offset,ALL_ANIME_FIELDS)
                try {
                    val userAnime = getUserAnimeDeferred.await()
                    val userAnimeList = userAnime.data
                    setUserAnimeData(userAnime)
                    userAnimeListDao.deleteUserAnimeById(animeId)
                    userAnimeListDao.insertUseAnimeList(userAnimeList!!)

                }catch (e:Exception){
                    withContext(Dispatchers.Main){
                    }
                }
            }
        }
    }

    private fun setUserAnimeData(userAnime: UserAnimeList){
        val userAnimeList = userAnime.data
        if (userAnimeList != null) {
            for (anime in userAnimeList){
                anime?.status = anime?.anime?.myAnimeListStatus?.status
                anime?.title =  anime?.anime?.title
                anime?.animeId = anime?.anime?.id
                val next = userAnime.paging?.next
                val prev = userAnime.paging?.previous
                anime?.offset = calcOffset(next, prev)
            }
        }
    }

    private fun calcOffset(nextPage: String?, prevPage:String?): String{
        var currentOffset = "0"
        if(!nextPage.isNullOrBlank()){
            val nextOffset = getOffset(nextPage)
            if (!nextOffset.isNullOrBlank()){
                val temp = nextOffset.toInt().minus(DEFAULT_USER_LIST_PAGE_LIMIT.toInt())
                if (temp>=0){
                    currentOffset = temp.toString()
                }
            }
            return currentOffset
        }else{
            val prevOffset = getOffset(prevPage)
            if (!prevOffset.isNullOrBlank()){
                val temp = prevOffset.toInt().plus(DEFAULT_USER_LIST_PAGE_LIMIT.toInt())
                if (temp>=0){
                    currentOffset = temp.toString()
                }
            }
            return currentOffset
        }

    }

    private fun getOffset(url: String?): String?{

        if (!url.isNullOrBlank()){
            val uri = url.toUri()
            return uri.getQueryParameter("offset").toString()
        }else{
            return null
        }
    }



    }