package ru.skillbranch.sbdelivery.screens.dish.logic

import android.util.Log
import ru.skillbranch.sbdelivery.aop.LogAspect
import ru.skillbranch.sbdelivery.aop.doMoreClean
import ru.skillbranch.sbdelivery.data.network.res.ReviewRes
import ru.skillbranch.sbdelivery.screens.dish.data.DishUiState
import ru.skillbranch.sbdelivery.screens.dish.data.ReviewUiState
import ru.skillbranch.sbdelivery.screens.root.logic.Eff
import ru.skillbranch.sbdelivery.screens.root.logic.RootState
import ru.skillbranch.sbdelivery.screens.root.logic.ScreenState

fun DishFeature.State.selfReduce(msg: DishFeature.Msg) : Pair<DishFeature.State, Set<Eff>> {
    Log.v(LogAspect.tag, ">>>--------DishFeature.State.selfReduce()")
    val pair = when (msg) {
        // Здесь msg.count - количество штук блюда, добавляемых в корзину за раз. При этом мы
        // в стейте сбрасываем свойство count (цифру в степпере) к 1
        is DishFeature.Msg.AddToCart -> copy(count = 1) to setOf(DishFeature.Eff.AddToCart(msg.id, msg.count)).toEffs()
        is DishFeature.Msg.DecrementCount -> copy(count = count - 1) to emptySet<Eff>()
        is DishFeature.Msg.HideReviewDialog -> copy(isReviewDialog = false) to emptySet<Eff>()
        is DishFeature.Msg.IncrementCount -> copy(count = count + 1) to emptySet<Eff>()
        is DishFeature.Msg.SendReview -> {
            val reviewList = if (reviews is ReviewUiState.Empty) emptyList()
                else (reviews as ReviewUiState.Content).list
            copy(isReviewDialog = false, reviews = ReviewUiState.ContentWhileLoading(reviewList)) to
                    setOf(DishFeature.Eff.SendReview(msg.dishId, msg.rating, msg.review, reviewList)).toEffs()
        }
        is DishFeature.Msg.ShowDish -> copy(content = DishUiState.Thing(msg.dish)) to emptySet<Eff>()
        is DishFeature.Msg.ShowReviewDialog -> copy(isReviewDialog = true) to emptySet<Eff>()
        is DishFeature.Msg.ShowReviews -> copy(reviews = ReviewUiState.Content(msg.reviews)) to emptySet<Eff>()
        is DishFeature.Msg.ReviewsEmpty -> copy(reviews = ReviewUiState.Empty) to emptySet<Eff>()
        // По хорошему тут еще надо добавить эффект ToggleLike, а в хендлере эффектов
        // записать в БД девайса, что данное блюдо лайкнуто владельцем девайса
        is DishFeature.Msg.ToggleLike -> copy(isLiked = !isLiked) to emptySet<Eff>()
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
