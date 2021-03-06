package ru.skillbranch.sbdelivery.screens.cart.logic

import ru.skillbranch.sbdelivery.aop.LogClassMethods
import ru.skillbranch.sbdelivery.screens.cart.data.CartItem
import ru.skillbranch.sbdelivery.screens.cart.data.CartUiState
import ru.skillbranch.sbdelivery.screens.cart.data.ConfirmDialogState
import java.io.Serializable

@LogClassMethods
object CartFeature {

    const val route = "cart"

    fun initialState() : State = State()
    fun initialEffects() : Set<Eff> = setOf(Eff.LoadCart)

    data class State(
        val promo: String = "",
        val confirmDialog: ConfirmDialogState = ConfirmDialogState.Hide,
        val uiState: CartUiState = CartUiState.Empty
    ): Serializable

    sealed class Eff {
        object LoadCart: Eff()
        data class  IncrementItem(val dishId: String): Eff()
        data class  DecrementItem(val dishId: String): Eff()
        data class RemoveItem(val id: String, val title:String): Eff()
        data class SendOrder(val order: Map<String, Int>) : Eff()
    }

    sealed class Msg {
        object HideConfirm : Msg()
        // order - мапа из пар (id блюда <--> количество штук этого блюда в корзине)
        data class SendOrder(val order: Map<String, Int>) : Msg()
        data class ShowConfirm(val id:String, val title: String) : Msg()
        data class IncrementCount(val dishId:String) : Msg()
        data class DecrementCount(val dishId:String) : Msg()
        data class ShowCart(val cart: List<CartItem>) : Msg()
        data class RemoveFromCart(val id: String) :Msg()
        data class ClickOnDish(val dishId: String, val title: String) :Msg()
    }
}