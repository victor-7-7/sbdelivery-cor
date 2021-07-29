package ru.skillbranch.sbdelivery.screens.root.logic

import ru.skillbranch.sbdelivery.screens.cart.logic.CartFeature
import ru.skillbranch.sbdelivery.screens.dish.logic.DishFeature

fun RootState.reduceNavigate(msg: NavigateCommand): Pair<RootState, Set<Eff>> {

    // t.c. 01:56:30 если текущий экран (с которого уходим) является
    // экраном "dish", то в сет закидываем эффект терминации корутин,
    // выполняющихся на экране в момент, когда мы уходим с экрана.
    // Эти корутины будут прерваны
    val navEffs : Set<Eff> = when(current){
        is ScreenState.Dish -> setOf(Eff.Dish(DishFeature.Eff.Terminate))
        else -> emptySet()
    }

    return when (msg) {
        // Была нажата либо кнопка "назад" в тулбаре (рут-экран не имеет этой кнопки),
        // либо системная кнопка back key девайса
        is NavigateCommand.ToBack -> {
            // t.c. 01:39:30 выбрасываем из бэкстека стейт текущего экрана,
            // ИЗ которого мы переходим на новый
            val newBackstack = backstack.dropLast(1)
            // Берем из бэкстека верхний стейт экрана (на который нам надо перейти)
            val newScreenState = backstack.lastOrNull()
            // Если бэкстек оказался пустым, значит мы были в рут-экране
            // и значит выходим из проги. Посылаем эффект на закрытие RootActivity
            if (newScreenState == null) this to setOf(Eff.Cmd(Command.Finish))
            else {
                // Создаем мутабельную мапу из свойства screens (Map<String, ScreenState>)
                // класса RootState. Элементы мапы представлют из себя название экрана [ключ]
                // и стейт экрана [значение].
                // sealed-классу ScreenState(val route: String, val title: String)
                // наследуют три класса ScreenState.Dishes, ScreenState.Dish, ScreenState.Cart.
                // Каждый из трех имеет дополнительное свойство state своего типа. Например:
                // Dishes(val state: DishesFeature.State) : ScreenState(DishesFeature.route, "Все блюда")
                // При этом DishesFeature имеет константное поле route = "dishes" и
                // экземпляр ScreenState.Dishes имеет свойство route = "dishes". Аналогично
                // для прочих экранов
                val newScreens = screens.toMutableMap()
                    // operator fun <K, V> MutableMap<K, V>.set(key: K, value: V)
                    // Allows to use the index operator for storing values in a mutable map
                    // Более короткая запись для оператора set -> mutableMap[key] = value
                    // Обновляем значение элемента мутабельной мапы по ключу этого элемента
                    .also { mutableScreens -> mutableScreens[newScreenState.route] = newScreenState }

                // Создаем новый экземпляр RootState с новыми свойствами (неуказанное
                // свойство cartCount останется с прежним значением) и формируем
                // итоговую пару для возврата из функции reduceNavigate
                copy(
                    screens = newScreens,
                    backstack = newBackstack,
                    currentRoute = newScreenState.route
                ) to emptySet()
                // После того как пара <новый стейт/эффекты> будет возвращена из reduceNavigate
                // она прокинется из reduceDispatcher в функцию scan на потоке mutations
            }
        }

        is NavigateCommand.ToCart -> {
            //return if on cart screen (single top)
            if(current.route === CartFeature.route) return this to emptySet()

            val newBackstack = backstack.plus(current)
            var newState = copy(currentRoute = CartFeature.route, backstack = newBackstack)
            newState = newState.changeCurrentScreen<ScreenState.Cart> {
                copy(state = CartFeature.initialState())
            }
            val newEffs = CartFeature.initialEffects().mapTo(HashSet(), Eff::Cart)
            // Итоговая пара
            newState to newEffs
        }

        is NavigateCommand.ToDishItem -> {
            val newBackstack = backstack.plus(current)
            var newState = copy(currentRoute = DishFeature.route, backstack = newBackstack)
            newState = newState.changeCurrentScreen<ScreenState.Dish> {
                copy(
                    state = DishFeature.State(
                        id = msg.id,
                        title = msg.title
                    )
                )
            }
            val newEffs = DishFeature.initialEffects(msg.id).mapTo(HashSet(), Eff::Dish)
            // Итоговая пара
            newState to newEffs
        }
    // Прежде чем вернуть пару из reduceNavigate() докидываем
    // в сет (второй элемент пары) элементы из сета navEffs
    }.run { first to second.plus(navEffs) }
}