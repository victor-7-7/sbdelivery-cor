package ru.skillbranch.sbdelivery.screens.dishes.data

import java.io.Serializable

sealed class DishesUiState: Serializable {
    object Loading: DishesUiState()
    object Empty:DishesUiState()
    object Error:DishesUiState()
    data class Things(val dishes: List<DishItem>) : DishesUiState()
}