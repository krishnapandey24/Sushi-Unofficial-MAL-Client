package com.destructo.sushi.ui.animeSchedule

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.destructo.sushi.model.jikan.schedule.Schedule
import com.destructo.sushi.network.Resource
import kotlinx.coroutines.launch

class ScheduleViewModel
@ViewModelInject
constructor(
    @Assisted
    savedStateHandle: SavedStateHandle,
    private val scheduleRepo: ScheduleRepository
)
    :ViewModel() {
    val animeSchedule:LiveData<Resource<Schedule>> = scheduleRepo.animeSchedule

    fun getAnimeSchedule(weekDay:String){
       viewModelScope.launch { scheduleRepo.getAnimeSchedule(weekDay) }
    }


}