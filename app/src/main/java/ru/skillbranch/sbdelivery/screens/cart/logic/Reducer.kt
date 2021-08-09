package ru.skillbranch.sbdelivery.screens.cart.logic

import android.util.Log
import ru.skillbranch.sbdelivery.aop.LogAspect
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
        is CartFeature.Msg.RemoveFromCart -> TODO()
        is CartFeature.Msg.SendOrder -> TODO()
        is CartFeature.Msg.ShowCart -> {
            if (msg.cart.isEmpty()) copy(uiState = CartUiState.Empty) to emptySet()
            else copy(uiState = CartUiState.Things(msg.cart)) to emptySet()
        }
        is CartFeature.Msg.ShowConfirm ->
            if (true) { // todo
                this to emptySet()
            } else {
                this to setOf(CartFeature.Eff.RemoveItem(msg.id)).toEffs()
            }
    }
    val msgV = "$msg".replace(LogAspect.regex, LogAspect.replacement)
    Log.v(LogAspect.tag,  "Params(selfReduce): [msg = $msgV] | Return Value: $pair")
    Log.v(LogAspect.tag, "<<<--------CartFeature.State.selfReduce()")
    return pair
}

fun CartFeature.State.reduce(root: RootState, msg: CartFeature.Msg): Pair<RootState, Set<Eff>> {
    Log.v(LogAspect.tag, ">>>--------CartFeature.State.reduce()")
    val (screenState, effs) = selfReduce(msg)
    val pair = root.changeCurrentScreen<ScreenState.Cart> { copy(state = screenState) } to effs
    val rootV = "$root".replace(LogAspect.regex, LogAspect.replacement)
    val msgV = "$msg".replace(LogAspect.regex, LogAspect.replacement)
    val pairF = "${pair.first}".replace(LogAspect.regex, LogAspect.replacement)
    val pairS = "${pair.second}".replace(LogAspect.regex, LogAspect.replacement)
    Log.v(LogAspect.tag,  "Params(reduce): [root = $rootV] [msg = $msgV] | Return Value: pairF => $pairF *** pairS => $pairS")
    Log.v(LogAspect.tag, "<<<--------CartFeature.State.reduce()")
    return pair
}

private fun Set<CartFeature.Eff>.toEffs(): Set<Eff> = mapTo(HashSet(), Eff::Cart)