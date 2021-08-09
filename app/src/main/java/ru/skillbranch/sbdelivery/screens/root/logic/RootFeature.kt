package ru.skillbranch.sbdelivery.screens.root.logic

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.skillbranch.sbdelivery.aop.LogClassMethods
import ru.skillbranch.sbdelivery.screens.cart.logic.CartFeature
import ru.skillbranch.sbdelivery.screens.cart.logic.reduce
import ru.skillbranch.sbdelivery.screens.dish.logic.DishFeature
import ru.skillbranch.sbdelivery.screens.dish.logic.reduce
import ru.skillbranch.sbdelivery.screens.dishes.logic.DishesFeature
import ru.skillbranch.sbdelivery.screens.dishes.logic.reduce
import java.io.Serializable

@LogClassMethods
object RootFeature {

    // Функция вызывается только при запуске приложения в двух местах.
    // 1. При инициализации _state. 2. При эмите первого мессиджа в поток мутаций
    private fun initialState(): RootState = RootState(
        screens = mapOf(
            DishesFeature.route to ScreenState.Dishes(DishesFeature.initialState()),
            DishFeature.route to ScreenState.Dish(DishFeature.initialState()),
            CartFeature.route to ScreenState.Cart(CartFeature.initialState()),
        ),
        currentRoute = DishesFeature.route // <-- "dishes"
    )

    // 35:07
    private fun initialEffects(): Set<Eff> =
        DishesFeature.initialEffects().mapTo(HashSet(), Eff::Dishes) + Eff.SyncCounter

    // StateFlow is a SharedFlow that represents a read-only state with a single
    // updatable data value that emits updates to the value to its collectors.
    // A state flow is a hot flow because its active instance exists independently
    // of the presence of collectors. Its current value can be retrieved via
    // the value property
    /** Горячий разделяемый поток стейтов RootState */
    private val _state: MutableStateFlow<RootState> = MutableStateFlow(initialState())
    // Свойство, доступное снаружи, должно быть read-only
    val state
        get() = _state.asStateFlow()

    private lateinit var _scope: CoroutineScope

    // SharedFlow is a hot Flow that shares emitted values among all its collectors
    // in a broadcast fashion, so that all collectors get all emitted values.
    // A shared flow is called hot because its active instance exists
    // independently of the presence of collectors. This is opposed to a
    // regular Flow which is cold and is started separately for each collector
    /** Горячий разделяемый поток сообщений Msg */
    private val mutations: MutableSharedFlow<Msg> = MutableSharedFlow()

    fun mutate(mutation: Msg) {
        _scope.launch {
            mutations.emit(mutation)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun listen(scope: CoroutineScope, effDispatcher: IEffHandler<Eff, Msg>, initState: RootState?) {
        Log.e("RootFeature", "Start listen init state: $initState")
        _scope = scope
        _scope.launch {
            mutations.onEach { Log.e("DemoEffHandler", "MUTATION $it") }
                // fun <T, R> Flow<T>.scan(initial: R, operation: suspend (R, T) -> R): Flow<R>
                // Folds the given flow with operation, emitting every intermediate result,
                // including initial value.
                // flowOf(1, 2, 3).scan(emptyList<Int>()) { acc, value -> acc + value }
                // will produce [], [1], [1, 2], [1, 2, 3]
                // При запуске приложения initState = null. Начальная пара для свертки:
                // RootState (мапа из трех экранов, текущий рут "dishes"...) и сет из
                // двух эффектов - SyncDishes и SyncCounter. Эта начальная пара будет
                // проэмичена в коллектор. Затем в коллектор будут эмитится пары, являющиеся
                // результатом свертки (в функции reduceDispatcher) текущей (проэмиченой
                // до того) пары и очередного мессиджа
                .scan((initState ?: initialState()) to initialEffects()) { (s, _), m ->
                    // При запуске приложения s = RootState(..., currentRoute=dishes,
                    // backstack=[], cartCount=0), а m = Dishes(msg=ShowDishes(dishes=
                    // [DishItem(...),DishItem...]))
                    Log.e("DemoEffHandler", "Before reduceDispatcher: s => $s\nm => $m")
                    reduceDispatcher(s, m)
                }
                .collect { (s, eff) ->
                    // При запуске приложения s = RootState(..., currentRoute=dishes,
                    // backstack=[], cartCount=0), а eff = сет из SyncDishes и SyncCounter.
                    // Эмитим очередной стейт, чтобы подписчики на поток стейтов среагировали
                    _state.emit(s)
                    // Разбираемся с очередным набором эффектов (в отдельных корутинах)
                    eff.forEach {
                        launch {
                            // Каждый эффект (из очередного набора) передаем в рутовый
                            // диспетчер в его метод handle вместе с блоком mutate
                            // (этот блок в отдельной корутине эмитит соответствующий
                            // мессидж в поток мутаций)
                            effDispatcher.handle(it, RootFeature::mutate)
                        }
                    }
                }
        }
    }

    private fun reduceDispatcher(root: RootState, msg: Msg): Pair<RootState, Set<Eff>> =
        when {
            // t.c. 01:53:30 поясняется зачем нужно проверять два условия
            msg is Msg.Dishes && root.current is ScreenState.Dishes ->
                root.current.state.reduce(root, msg.msg)

            msg is Msg.Dish && root.current is ScreenState.Dish ->
                root.current.state.reduce(root, msg.msg)

            msg is Msg.Cart && root.current is ScreenState.Cart ->
                root.current.state.reduce(root, msg.msg)

            //root mutations
            msg is Msg.UpdateCartCount -> root.copy(cartCount = msg.count) to emptySet()
            //navigation
            msg is Msg.Navigate -> root.reduceNavigate(msg.cmd)

            else -> root to emptySet()
        }
}

@LogClassMethods
data class RootState(
    val screens: Map<String, ScreenState>,
    val currentRoute: String,
    val backstack: List<ScreenState> = emptyList(),
    val cartCount: Int = 0
) : Serializable {
    val current: ScreenState = checkNotNull(screens[currentRoute])

    fun <T : ScreenState> changeCurrentScreen(block: T.() -> T): RootState {
        val newScreen = (current as? T)?.block()
        val newScreens = if (newScreen != null) screens.toMutableMap().also { mutScreens ->
            mutScreens[currentRoute] = newScreen
        } else screens
        return copy(screens = newScreens)
    }
}

@LogClassMethods
sealed class ScreenState(
    val route: String,
    val title: String
) : Serializable {
    data class Dishes(val state: DishesFeature.State) :
        ScreenState(DishesFeature.route, "Все блюда")

    data class Dish(val state: DishFeature.State) :
        ScreenState(DishFeature.route, state.title)

    data class Cart(val state: CartFeature.State) :
        ScreenState(CartFeature.route, "Корзина")
}

@LogClassMethods
sealed class Msg {
    data class Dishes(val msg: DishesFeature.Msg) : Msg()
    data class Dish(val msg: DishFeature.Msg) : Msg()
    data class Cart(val msg: CartFeature.Msg) : Msg()

    //Navigation in root state level
    data class Navigate(val cmd: NavigateCommand) : Msg()

    //Root mutation
    data class UpdateCartCount(val count: Int) : Msg()
}

@LogClassMethods
sealed class Eff {
    data class Dishes(val eff: DishesFeature.Eff) : Eff()
    data class Dish(val eff: DishFeature.Eff) : Eff()
    data class Cart(val eff: CartFeature.Eff) : Eff()

    sealed class Notification(open val message: String) : Eff() {
        data class Text(override val message: String) : Notification(message)
        data class Action(override val message: String, val label: String, val action: Msg) :
            Notification(message)

        data class Error(
            override val message: String,
            val label: String? = null,
            val action: Msg? = null
        ) : Notification(message)
    }

    //root effects
    object SyncCounter : Eff()

    data class Navigate(val cmd: NavigateCommand) : Eff()
    data class Cmd(val cmd: Command) : Eff()
}

@LogClassMethods
sealed class NavigateCommand {
    data class ToDishItem(val id: String, val title: String) : NavigateCommand()
    object ToCart : NavigateCommand()
    object ToBack : NavigateCommand()
}

@LogClassMethods
sealed class Command {
    object Finish : Command()
    //Android specific commands Activity::finish(), startForResult, etc
}
