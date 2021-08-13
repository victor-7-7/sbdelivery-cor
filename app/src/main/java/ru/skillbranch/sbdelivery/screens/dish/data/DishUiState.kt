package ru.skillbranch.sbdelivery.screens.dish.data

import java.io.Serializable

sealed class DishUiState : Serializable {
    object Loading : DishUiState()
    data class Thing(val dishContent: DishContent) : DishUiState()
}
