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

package com.example.android.trackmysleepquality.sleepquality

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import kotlinx.coroutines.*

class SleepQualityViewModel(
        val nightId: Long,
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application){

    private var viewModelJob = Job()

    // This is the main scope. It is associated with the Main Dispatcher and our Job
    // We associated with the Main dispatcher because we want to automatically notify the view of changes
    private val uiScope = CoroutineScope(Dispatchers.Main +  viewModelJob)

    private val _doneQualifying = MutableLiveData<Boolean>()
    val doneQualifying: LiveData<Boolean>
        get() = _doneQualifying

    fun qualifyNight(qualification: Int){
        uiScope.launch {
            withContext(Dispatchers.IO){
                val night = database.get(nightId)
                night?.let {actualNight ->
                    actualNight.sleepQuality = qualification
                    database.update(actualNight)
                }
            }
            _doneQualifying.value = true
        }
    }

    fun doneNavigating(){
        _doneQualifying.value = false
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}