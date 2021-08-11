package ru.skillbranch.sbdelivery.screens.root.ui

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import ru.skillbranch.sbdelivery.RootViewModel
import ru.skillbranch.sbdelivery.screens.root.logic.RootState
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.collect
import ru.skillbranch.sbdelivery.screens.cart.ui.CartScreen
import ru.skillbranch.sbdelivery.screens.dish.ui.DishScreen
import ru.skillbranch.sbdelivery.screens.dishes.ui.DishesScreen
import ru.skillbranch.sbdelivery.screens.dishes.ui.DishesToolbar
import ru.skillbranch.sbdelivery.screens.root.logic.Eff
import ru.skillbranch.sbdelivery.screens.root.logic.Msg
import ru.skillbranch.sbdelivery.screens.root.logic.ScreenState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import ru.skillbranch.sbdelivery.aop.LogAspect
import ru.skillbranch.sbdelivery.aop.doMoreClean

@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@Composable
fun RootScreen(vm: RootViewModel) {
    Log.w(LogAspect.tag, ">>>--------RootScreen() Params: [vm = RootViewModel]")
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    // t.c. 01:28:21 так как нет изменяемых аргументов, то можно использовать
    // scope.launch { ...
    LaunchedEffect(scope) {
        vm.dispatcher.notifications
            .collect { notification -> renderNotification(notification, scaffoldState, vm::accept) }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { AppbarHost(vm) },
        content = { ContentHost(vm) }
    )
    Log.w(LogAspect.tag, "<<<--------RootScreen()")
}

@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@Composable
fun ContentHost(vm: RootViewModel) {
    Log.w(LogAspect.tag, ">>>--------ContentHost() Params: [vm = RootViewModel]")
    // collectAsState => Collects values from this StateFlow and represents its
    // latest value via State. The StateFlow.value is used as an initial value.
    // Every time there would be new value posted into the StateFlow the returned
    // State will be updated causing recomposition of every State.value usage
    val state: RootState by vm.feature.state.collectAsState()
    val screenState: ScreenState = state.currentScrSt
    // t.c. 02:16:00 при возврате на предыдущий скрин, если его стейт не изменился,
    // то фреймворк подставит уже построенный ранее Composable. А если изменилось
    // свойство Х стейта, то фреймворк перерисует только тот компонент лейаута в
    // Composable, на который свойство Х влияет
    Navigation(currScrSt = screenState, modifier = Modifier.fillMaxSize()) { currScrSt ->
        // Здесь формируется @Composable представление контент-хоста. На этот блок кода
        // ссылается параметр content в функции Navigation()
        when (currScrSt) {
            // В зависимости от типа скринстейта выбираем соответствующий композбл и
            // передаем в него соответствующий ему стейт (напр - DishesFeature.State) и
            // лямбду (под именем accept), принимающую мессиджи из соответствующей группы
            // (напр - из DishesFeature.Msg). Эта лямбда будет вызвана внутри соотв
            // композбла. Например, в DishesScreen она будет вызвана по клику на блюде так:
            // onClick = { dish -> accept(DishesFeature.Msg.ClickDish(dish.id, dish.title)) }.
            // Это приведет к тому, что будет вызван метод accept() вьюмодели с
            // передачей в него мессиджа DishesFeature.Msg.ClickDish и заработает реактивщина
            is ScreenState.Dishes -> DishesScreen(currScrSt.dishesState) { vm.accept(Msg.Dishes(it)) }
            is ScreenState.Dish -> DishScreen(currScrSt.dishState) { vm.accept(Msg.Dish(it)) }
            is ScreenState.Cart -> CartScreen(currScrSt.cartState) { vm.accept(Msg.Cart(it)) }
        }
    }
    Log.w(LogAspect.tag, "<<<--------ContentHost()")
}

@Composable
fun Navigation(
    currScrSt: ScreenState,
    modifier: Modifier = Modifier,
    content: @Composable (ScreenState) -> Unit
){
    Log.w(LogAspect.tag, ">>>--------Navigation() Params: [currScrSt = $currScrSt]".doMoreClean())
    // t.c. 02:17:00 как бы "локальная" переменная функции. На самом деле
    // ее значение будет восстановлено при вызове функции в то значение,
    // которое она имела при предыдущем вызове функции
    val restorableStateHolder = rememberSaveableStateHolder()
    // t.c. 02:15:00 пояснения про отличие Composable от Widgets
    Box(modifier){
        // SaveableStateProvider ->
        // Put your content associated with a key inside the content.
        // This will automatically save all the states defined with
        // rememberSaveable before disposing the content and will restore
        // the states when you compose with this key again
        restorableStateHolder.SaveableStateProvider(key = currScrSt.route + currScrSt.title) {
            // Построенное в лямбде content-представление Composable, будет
            // закешировано в restorableStateHolder с ключом key при уходе с экрана
            content(currScrSt)
        }
    }
    Log.w(LogAspect.tag, "<<<--------Navigation()")
}

@ExperimentalComposeUiApi
@Composable
fun AppbarHost(vm: RootViewModel) {
    Log.w(LogAspect.tag, ">>>--------AppbarHost() Params: [vm = RootViewModel]")
    // collectAsState => Collects values from this StateFlow and represents its
    // latest value via State. The StateFlow.value is used as an initial value.
    // Every time there would be new value posted into the StateFlow the returned
    // State will be updated causing recomposition of every State.value usage
    val state: RootState by vm.feature.state.collectAsState()
    when (val screen: ScreenState = state.currentScrSt) {
        is ScreenState.Dishes -> DishesToolbar(
            state = screen.dishesState,
            cartCount = state.cartCount,
            accept = { vm.accept(Msg.Dishes(it)) },
            navigate = vm::navigate
        )
        else -> DefaultToolbar(title = screen.title, cartCount = state.cartCount, navigate =  vm::navigate  )
    }
    Log.w(LogAspect.tag, "<<<--------AppbarHost()")
}

private suspend fun renderNotification(
    notification: Eff.Notification,
    scaffoldState: ScaffoldState,
    accept: (Msg) -> Unit
) {
    Log.w(LogAspect.tag, ">>>--------RootScreen.renderNotification()")
    // В компоузе метод showSnackbar показывает бар и затем возвращает результат
    // SnackbarResult.Dismissed (если истек таймаут или юзер смахнул снэкбар)
    // или SnackbarResult.ActionPerformed (если юзер тапнул кнопку экшн)
    val result = when(notification){
        is Eff.Notification.Text -> {
            Log.w(LogAspect.tag, "Show Text Notification. Snackbar: [message = ${notification.message}]")
            scaffoldState.snackbarHostState.showSnackbar(notification.message)
        }
        is Eff.Notification.Action -> {
            val (message, label) = notification
            Log.w(LogAspect.tag, "Show Action Notification. Snackbar: [message = $message] [actionLabel = $label]")
            scaffoldState.snackbarHostState.showSnackbar(message, label)
        }
        is Eff.Notification.Error -> {
            val (message, label) = notification
            scaffoldState.snackbarHostState.showSnackbar(message, label)
        }
    }

    when(result){
        SnackbarResult.Dismissed -> { /*Nothing*/ }
        SnackbarResult.ActionPerformed -> {
            when(notification){
                is Eff.Notification.Action -> {
                    Log.w(LogAspect.tag, "Action Notification Result: accept [action = ${notification.action}]")
                    accept(notification.action)
                }
                is Eff.Notification.Error -> notification.action?.let { accept(it) }
                else  ->  { /*Nothing*/ }
            }
        }
    }
    Log.w(LogAspect.tag, "<<<--------RootScreen.renderNotification()")
}
