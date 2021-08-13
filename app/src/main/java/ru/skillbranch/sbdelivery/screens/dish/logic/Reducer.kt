package ru.skillbranch.sbdelivery.screens.dish.logic

import android.util.Log
import ru.skillbranch.sbdelivery.aop.LogAspect
import ru.skillbranch.sbdelivery.aop.doMoreClean
import ru.skillbranch.sbdelivery.screens.dish.data.DishUiState
import ru.skillbranch.sbdelivery.screens.dish.data.ReviewUiState
import ru.skillbranch.sbdelivery.screens.root.logic.Eff
import ru.skillbranch.sbdelivery.screens.root.logic.RootState
import ru.skillbranch.sbdelivery.screens.root.logic.ScreenState

fun DishFeature.State.selfReduce(msg: DishFeature.Msg) : Pair<DishFeature.State, Set<Eff>> {
    Log.v(LogAspect.tag, ">>>--------DishFeature.State.selfReduce()")
    val pair = when (msg) {
        is DishFeature.Msg.AddToCart -> TODO()
        is DishFeature.Msg.DecrementCount -> TODO()
        is DishFeature.Msg.HideReviewDialog -> TODO()
        is DishFeature.Msg.IncrementCount -> TODO()
        is DishFeature.Msg.SendReview -> TODO()
        is DishFeature.Msg.ShowDish -> copy(content = DishUiState.Thing(msg.dish)) to emptySet<Eff>()
        is DishFeature.Msg.ShowReviewDialog -> TODO()
        is DishFeature.Msg.ShowReviews -> copy(reviews = ReviewUiState.Content(msg.reviews)) to emptySet<Eff>()
        is DishFeature.Msg.ToggleLike -> {
            copy(content = DishUiState.Thing((content as DishUiState.Thing).dishContent
                .copy(isLiked = !content.dishContent.isLiked))) to emptySet<Eff>()
        }
    }
    val pairV = "$pair".replace("ru.skillbranch.sbdelivery.screens.", "")
    Log.v(LogAspect.tag,  "Params(selfReduce): [msg = $msg]| Return Value: $pairV")
    Log.v(LogAspect.tag, "<<<--------DishFeature.State.selfReduce()")
    return pair
}

fun  DishFeature.State.reduce(root: RootState, msg: DishFeature.Msg) : Pair<RootState, Set<Eff>> {
    Log.v(LogAspect.tag, ">>>--------DishFeature.State.reduce()")
    val (dishState, effs) = selfReduce(msg)
    // Блок copy(dishState = dishState) будет выполнен на экземпляре ScreenState.Dish,
    // который имеет свойство dishState типа DishFeature.State
    val pair = root.updateCurrentScreenState<ScreenState.Dish> { copy(dishState = dishState) } to effs
    val rootV = "$root".doMoreClean()
    val msgV = "$msg".doMoreClean()
    val pairF = "${pair.first}".doMoreClean()
    val pairS = "${pair.second}".doMoreClean()
    Log.v(LogAspect.tag,  "Params(reduce): [root = $rootV] [msg = $msgV] | Return Value: pairF => $pairF *** pairS => $pairS")
    Log.v(LogAspect.tag, "<<<--------DishFeature.State.reduce()")
    return pair
}

private fun Set<DishFeature.Eff>.toEffs(): Set<Eff> = mapTo(HashSet(), Eff::Dish)
