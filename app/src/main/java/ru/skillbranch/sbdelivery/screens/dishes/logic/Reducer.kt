package ru.skillbranch.sbdelivery.screens.dishes.logic

import android.util.Log
import ru.skillbranch.sbdelivery.aop.LogAspect
import ru.skillbranch.sbdelivery.aop.doMoreClean
import ru.skillbranch.sbdelivery.screens.dishes.data.DishesUiState
import ru.skillbranch.sbdelivery.screens.root.logic.Eff
import ru.skillbranch.sbdelivery.screens.root.logic.NavigateCommand
import ru.skillbranch.sbdelivery.screens.root.logic.RootState
import ru.skillbranch.sbdelivery.screens.root.logic.ScreenState

fun DishesFeature.State.selfReduce(msg: DishesFeature.Msg): Pair<DishesFeature.State, Set<Eff>> {
    Log.v(LogAspect.tag, ">>>--------DishesFeature.State.selfReduce()")
    val pair = when (msg) {
        is DishesFeature.Msg.AddToCart -> this to setOf(
            DishesFeature.Eff.AddToCart(
                msg.id,
                msg.title
            )
        ).toEffs()

        is DishesFeature.Msg.RemoveFromCart -> this to setOf(
            DishesFeature.Eff.RemoveFromCart(
                msg.id,
                msg.title
            )
        ).toEffs()

        is DishesFeature.Msg.ClickDish -> copy(
            isSearch = false,
            suggestions = emptyMap(),
            input = ""
        ) to setOf(
            Eff.Navigate(NavigateCommand.ToDishItem(msg.id, msg.title))
        )

        is DishesFeature.Msg.SearchInput -> copy(input = msg.newInput) to emptySet()

        is DishesFeature.Msg.SearchSubmit -> copy(uiState = DishesUiState.Loading) to setOf(
            DishesFeature.Eff.SearchDishes(msg.query)
        ).toEffs()

        is DishesFeature.Msg.SearchToggle -> when {
            input.isNotEmpty() && isSearch -> copy(input = "", suggestions = emptyMap()) to
                    setOf(DishesFeature.Eff.FindAllDishes).toEffs()
            input.isEmpty() && !isSearch -> copy(isSearch = true) to emptySet()
            else -> copy(isSearch = false, suggestions = emptyMap()) to emptySet()
        }

        is DishesFeature.Msg.ShowDishes -> {
            val dishes =
                if (msg.dishes.isEmpty()) DishesUiState.Empty else DishesUiState.Things(msg.dishes)
            copy(uiState = dishes, suggestions = emptyMap()) to emptySet()
        }
        is DishesFeature.Msg.ShowError -> TODO()
        is DishesFeature.Msg.ShowLoading -> copy(uiState = DishesUiState.Loading) to emptySet()
        is DishesFeature.Msg.ShowSuggestions -> copy(suggestions = msg.suggestions) to emptySet()

        is DishesFeature.Msg.SuggestionSelect -> {
            copy(suggestions = emptyMap(), input = msg.suggestion) to
                    setOf(DishesFeature.Eff.SearchDishes(msg.suggestion)).toEffs()
        }

        is DishesFeature.Msg.UpdateSuggestionResult -> this to
                setOf(DishesFeature.Eff.FindSuggestions(msg.query)).toEffs()
    }
    val msgV = "$msg".doMoreClean()
    val pairF = "${pair.first}".doMoreClean()
    val pairS = "${pair.second}".doMoreClean()
    Log.v(LogAspect.tag,  "Params(selfReduce): [msg = $msgV] | Return Value: pairF => $pairF *** pairS => $pairS")
    Log.v(LogAspect.tag, "<<<--------DishesFeature.State.selfReduce()")
    return pair
}

fun DishesFeature.State.reduce(root: RootState, msg: DishesFeature.Msg): Pair<RootState, Set<Eff>> {
    Log.v(LogAspect.tag, ">>>--------DishesFeature.State.reduce()")
    val (dishesState, effs) = selfReduce(msg)
    // Блок copy(dishesState = dishesState) будет выполнен на экземпляре ScreenState.Dishes,
    // который имеет свойство dishesState типа DishesFeature.State
    val pair = root.updateCurrentScreenState<ScreenState.Dishes> { copy(dishesState = dishesState) } to effs
    val rootV = "$root".doMoreClean()
    val msgV = "$msg".doMoreClean()
    val pairF = "${pair.first}".doMoreClean()
    val pairS = "${pair.second}".doMoreClean()
    Log.v(LogAspect.tag,  "Params(reduce): [root = $rootV] [msg = $msgV] | Return Value: pairF => $pairF *** pairS => $pairS")
    Log.v(LogAspect.tag, "<<<--------DishesFeature.State.reduce()")
    return pair
}

private fun Set<DishesFeature.Eff>.toEffs(): Set<Eff> = mapTo(HashSet(), Eff::Dishes)