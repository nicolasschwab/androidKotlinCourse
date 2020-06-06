/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import android.text.Spanned
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        // We receive the database as class parameter
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {

    // The job is the wrapper of everything associated with the coroutine
    private var viewModelJob = Job()

    // This is the main scope. It is associated with the Main Dispatcher and our Job
    // We associated with the Main dispatcher because we want to automatically notify the view of changes
    private val uiScope = CoroutineScope(Dispatchers.Main +  viewModelJob)

    private var tonight = MutableLiveData<SleepNight?>()

    private val nights = database.getAllNights()

    val nightsString: LiveData<Spanned> = Transformations.map(nights) { nights ->
        formatNights(nights, application.resources)
    }

    private val _nightAlreadyStarted = MutableLiveData<Boolean>()
    val nightAlreadyStarted: LiveData<Boolean>
        get() = _nightAlreadyStarted

    private val _noNightInitialized = MutableLiveData<Boolean>()
    val noNightInitialized: LiveData<Boolean>
        get() = _noNightInitialized

    private val _navigateToSleepQuality = MutableLiveData<SleepNight>()
    val navigateToSleepQuality: LiveData<SleepNight>
        get() = _navigateToSleepQuality

    private val _showClearedSnackbar = MutableLiveData<Boolean>()
    val showClearedSnackbar: LiveData<Boolean>
        get() = _showClearedSnackbar

    fun doneShowingClearedTackle(){
        _showClearedSnackbar.value = false
    }

    init {
        initializeTonight()
    }

    fun doneNavigatingToSleepQuality(){
        _navigateToSleepQuality.value = null
    }

    private fun initializeTonight(){
        uiScope.launch {
            tonight.value = getTonightFromDatabase()
        }
    }

    val startButtonEnable: LiveData<Boolean> = Transformations.map(tonight){
        it == null
    }

    val stopButtonEnable: LiveData<Boolean> = Transformations.map(tonight){
        it != null
    }

    val clearButtonEnable: LiveData<Boolean> = Transformations.map(nights){
        it?.isNotEmpty()
    }

    private suspend fun getTonightFromDatabase(): SleepNight?{
        return withContext(Dispatchers.IO){
            var tonightFromDb = database.getTonight()
            if(tonightFromDb?.startTimeMilli != tonightFromDb?.endTimeMilli){
                tonightFromDb = null
            }
            tonightFromDb
        }
    }

    fun onStartTracking(){
        uiScope.launch {
            if(tonight.value == null){
                createNewNightInDatabase()
                tonight.value = getTonightFromDatabase()
            }else{
                _nightAlreadyStarted.value = true
            }
        }
    }

    private suspend fun createNewNightInDatabase(){
        return withContext(Dispatchers.IO){
            val newNight = SleepNight()
            database.insert(newNight)
        }
    }

    fun nightAlreadyStartedAcknowledge(){
        _nightAlreadyStarted.value = false
    }

    fun onStopTracking(){
        uiScope.launch {
            val night = tonight.value ?: run {
                _noNightInitialized.value = true
                return@launch
            }
            night.endTimeMilli = System.currentTimeMillis()
            updateNightInDatabase(night)
            tonight.value = null
            _navigateToSleepQuality.value = night
        }
    }

    private suspend fun updateNightInDatabase(night: SleepNight){
        withContext(Dispatchers.IO){
            database.update(night)
        }
    }

    fun noNightInitializedAcknowledge(){
        _noNightInitialized.value = false
    }

    fun onClear(){
        uiScope.launch {
            clearAllNightsInDatabase()
        }
        _showClearedSnackbar.value = true
    }

    private suspend fun clearAllNightsInDatabase(){
        withContext(Dispatchers.IO){
            database.clear()
        }
    }

    // We should cancel every possible coroutine associated with this viewModel when it is cleared
    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}

