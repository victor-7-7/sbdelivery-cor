package ru.skillbranch.sbdelivery

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.skillbranch.sbdelivery.aop.LogClassMethods
import ru.skillbranch.sbdelivery.screens.root.logic.*
import javax.inject.Inject

@LogClassMethods
@HiltViewModel
class RootViewModel @Inject constructor(
    private val savedState : SavedStateHandle,
    val dispatcher: EffDispatcher
) : ViewModel() {
    val feature = RootFeature

    init {
        // При запуске приложения метод get вернет null
        val initState = savedState.get<RootState>("rootState")
        Log.e("RootViewModel", "initState")
        feature.listen(viewModelScope, dispatcher, initState)
    }

    fun accept(msg: Msg){
        feature.mutate(msg)
    }

    fun navigate(cmd: NavigateCommand){
        feature.mutate(Msg.Navigate(cmd))
    }

    fun saveState() {
        Log.e("RootViewModel", "save State: ${feature.state.value}")
        savedState["rootState"] = feature.state.value
    }

}