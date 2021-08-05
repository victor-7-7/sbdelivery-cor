package ru.skillbranch.sbdelivery.screens.cart.logic

import ru.skillbranch.sbdelivery.screens.cart.data.CartUiState
import ru.skillbranch.sbdelivery.screens.root.logic.Eff
import ru.skillbranch.sbdelivery.screens.root.logic.NavigateCommand
import ru.skillbranch.sbdelivery.screens.root.logic.RootState
import ru.skillbranch.sbdelivery.screens.root.logic.ScreenState

fun CartFeature.State.selfReduce(msg: CartFeature.Msg): Pair<CartFeature.State, Set<Eff>> =
    when (msg) {
        is CartFeature.Msg.ClickOnDish -> {
            this to setOf(Eff.Navigate(NavigateCommand.ToDishItem(msg.dishId, msg.title)))
        }
        is CartFeature.Msg.DecrementCount -> this to setOf(CartFeature.Eff.DecrementItem(msg.dishId)).toEffs()
        is CartFeature.Msg.IncrementCount -> this to setOf(CartFeature.Eff.IncrementItem(msg.dishId)).toEffs()
        is CartFeature.Msg.HideConfirm -> TODO()
        is CartFeature.Msg.RemoveFromCart -> TODO()
        is CartFeature.Msg.SendOrder -> TODO()
        is CartFeature.Msg.ShowCart -> {
            if (msg.cart.isEmpty()) copy(uiState = CartUiState.Empty) to emptySet()
            else copy(uiState = CartUiState.Content(msg.cart)) to emptySet()
        }
        is CartFeature.Msg.ShowConfirm ->
            if (true) {
                this to emptySet()
            } else {
                this to setOf(CartFeature.Eff.RemoveItem(msg.id)).toEffs()
            }

    }

fun CartFeature.State.reduce(root: RootState, msg: CartFeature.Msg): Pair<RootState, Set<Eff>> {
    val (screenState, effs) = selfReduce(msg)
    return root.changeCurrentScreen<ScreenState.Cart> { copy(state = screenState) } to effs
}

private fun Set<CartFeature.Eff>.toEffs(): Set<Eff> = mapTo(HashSet(), Eff::Cart)