package ru.skillbranch.sbdelivery.screens.dish.data

import ru.skillbranch.sbdelivery.data.network.res.ReviewRes
import java.io.Serializable

sealed class ReviewUiState : Serializable {
    object Loading : ReviewUiState()
    // Отзывы покупателей
    data class Content(val list: List<ReviewRes>) : ReviewUiState()
    object Empty : ReviewUiState()
}