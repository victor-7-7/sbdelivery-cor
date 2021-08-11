package ru.skillbranch.sbdelivery.screens.cart.logic

import android.util.Log
import ru.skillbranch.sbdelivery.aop.LogAspect
import ru.skillbranch.sbdelivery.aop.doMoreClean
import ru.skillbranch.sbdelivery.screens.cart.data.CartUiState
import ru.skillbranch.sbdelivery.screens.root.logic.Eff
import ru.skillbranch.sbdelivery.screens.root.logic.NavigateCommand
import ru.skillbranch.sbdelivery.screens.root.logic.RootState
import ru.skillbranch.sbdelivery.screens.root.logic.ScreenState

fun CartFeature.State.selfReduce(msg: CartFeature.Msg): Pair<CartFeature.State, Set<Eff>> {
    Log.v(LogAspect.tag, ">>>--------CartFeature.State.selfReduce()")
    val pair = when (msg) {
        is CartFeature.Msg.ClickOnDish -> {
            this to setOf(Eff.Navigate(NavigateCommand.ToDishItem(msg.dishId, msg.title)))
        }
        is CartFeature.Msg.DecrementCount -> this to setOf(CartFeature.Eff.DecrementItem(msg.dishId)).toEffs()
        is CartFeature.Msg.IncrementCount -> this to setOf(CartFeature.Eff.IncrementItem(msg.dishId)).toEffs()
        is CartFeature.Msg.HideConfirm -> TODO()
        is CartFeature.Msg.RemoveFromCart -> this to setOf(
            CartFeature.Eff.RemoveItem(
                msg.id,
                msg.title
            )
        ).toEffs()
        is CartFeature.Msg.SendOrder -> TODO()
        is CartFeature.Msg.ShowCart -> {
            if (msg.cart.isEmpty()) copy(uiState = CartUiState.Empty) to emptySet()
            else copy(uiState = CartUiState.Things(msg.cart)) to emptySet()
        }
        is CartFeature.Msg.ShowConfirm ->
            if (true) { // todo
                this to emptySet()
            } else {
                this to setOf(CartFeature.Eff.RemoveItem(msg.id, msg.title)).toEffs()
            }
    }
    val msgV = "$msg".doMoreClean()
    val pairV = "$pair".replace("ru.skillbranch.sbdelivery.screens.", "")
    Log.v(LogAspect.tag,  "Params(selfReduce): [msg = $msgV] | Return Value: $pairV")
    Log.v(LogAspect.tag, "<<<--------CartFeature.State.selfReduce()")
    return pair
}

fun CartFeature.State.reduce(root: RootState, msg: CartFeature.Msg): Pair<RootState, Set<Eff>> {
    Log.v(LogAspect.tag, ">>>--------CartFeature.State.reduce()")
    val (cartState, effs) = selfReduce(msg)
    // Блок copy(cartState = cartState) будет выполнен на экземпляре ScreenState.Cart,
    // который имеет свойство cartState типа CartFeature.State
    val pair = root.updateCurrentScreenState<ScreenState.Cart> { copy(cartState = cartState) } to effs
    val rootV = "$root".doMoreClean()
    val msgV = "$msg".doMoreClean()
    val pairF = "${pair.first}".doMoreClean()
    val pairS = "${pair.second}".doMoreClean()
    Log.v(LogAspect.tag,  "Params(reduce): [root = $rootV] [msg = $msgV] | Return Value: pairF => $pairF *** pairS => $pairS")
    Log.v(LogAspect.tag, "<<<--------CartFeature.State.reduce()")
    return pair
}

private fun Set<CartFeature.Eff>.toEffs(): Set<Eff> = mapTo(HashSet(), Eff::Cart)