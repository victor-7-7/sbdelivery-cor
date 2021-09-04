package ru.skillbranch.sbdelivery.repository

import android.util.Log
import ru.skillbranch.sbdelivery.aop.LogClassMethods
import ru.skillbranch.sbdelivery.data.db.dao.CartDao
import ru.skillbranch.sbdelivery.data.db.dao.DishesDao
import ru.skillbranch.sbdelivery.data.db.entity.CartItemPersist
import ru.skillbranch.sbdelivery.data.network.RestService
import ru.skillbranch.sbdelivery.data.network.res.DishRes
import ru.skillbranch.sbdelivery.data.toDishItem
import ru.skillbranch.sbdelivery.data.toDishPersist
import ru.skillbranch.sbdelivery.screens.dishes.data.DishItem
import javax.inject.Inject

interface IDishesRepository {
    suspend fun searchDishes(query: String): List<DishItem>
    suspend fun isEmptyDishes(): Boolean
    suspend fun syncDishes()
    suspend fun findDishes(): List<DishItem>
    suspend fun findSuggestions(query: String): Map<String, Int>
    suspend fun addDishToCart(dishId: String)
    suspend fun decrementOrRemoveDishFromCart(dishId: String)
    suspend fun cartCount(): Int
}

@LogClassMethods
class DishesRepository @Inject constructor(
    private val api: RestService,
    private val dishesDao: DishesDao,
    private val cartDao: CartDao
) : IDishesRepository {

    override suspend fun searchDishes(query: String): List<DishItem> {
        return if (query.isBlank()) findDishes()
        else dishesDao.findDishesFrom(query)
            .map { it.toDishItem() }
    }

    override suspend fun isEmptyDishes(): Boolean = dishesDao.dishesCounts() == 0

    override suspend fun syncDishes() {
        val dishes = mutableListOf<DishRes>()
        var offset = 0
        while (true) {
            val resp = api.getDishes(offset * 10, 10)
            if (resp.isSuccessful) {
                offset++
                dishes.addAll(resp.body()!!)
            } else break
        }
        dishesDao.insertDishes(dishes.map { it.toDishPersist() })
    }

    override suspend fun findDishes(): List<DishItem> =
        dishesDao.findAllDishes().map { it.toDishItem() }

    override suspend fun findSuggestions(query: String): Map<String, Int> {
        // Надо вернуть мапу из пар (ключ/значение). Ключ - название блюда
        // (или часть этого названия), содержащее в себе query-подстроку.
        // Значение - количество вхождений этой query-подстроки в названиях блюд.
        val suggs = mutableMapOf<String, Int>()
        if (query.isBlank()) return suggs // Пустые запросы отбрасываем

        else dishesDao.findDishesFrom(query)
            .map { dish ->
                // Индекс символа в dish.name, с которого начинаем искать
                var j = 0
                // Границы слова. end - это индекс следующего после слова символа, либо,
                // если слово оканчивает dish.name строку, end = dish.name.length
                var start: Int; var end : Int
                var shortName: String; var badLast = false
                // Ищем все вхождения query-подстроки в имени блюда dish
                while (j < dish.name.length) {
                    // Индекс первого вхождения query-подстроки в имя блюда, начиная с индекса j
                    j = dish.name.indexOf(query.trim(), j, ignoreCase = true)
                    // Если ничего не найдено, выходим из этой итерации
                    if (j == -1) break

                    start = j
                    // Ищем индекс символа, начинающего слово, содержащее query-подстроку
                    while (start > 0) {
                        // Смотрим на предыдущий символ. Если он не годится для слова, то выходим
                        if (dish.name[start - 1] == ' ' || dish.name[start - 1] == ','
                            || dish.name[start - 1] == '"' || dish.name[start - 1] == '.'
                            || dish.name[start - 1] == ';') break
                        // Берем символ в собираемое слово
                        start--
                    }

                    // Сдвигаем end на следующий после query-подстроки символ
                    end = j + query.trim().length
                    // Ищем индекс символа, следующего сразу ЗА словом, содержащим query-подстроку
                    while (end < dish.name.length) {
                        // Проверяем, пригоден ли end-символ для слова
                        if (dish.name[end] == ' ' || dish.name[end] == ',' || dish.name[end] == '"'
                            || dish.name[end] == '.' || dish.name[end] == ';')
                        {
                            // Если непригодный для слова символ оканчивает строку dish.name,
                            // то чтобы не зациклится делаем инкремент перед брейком
                            if (end == dish.name.length - 1) {
                                end++
                                badLast = true
                            }
                            break
                        }
                        // Берем символ в собираемое слово
                        end++
                    }
                    j = end

                    shortName = if (end == dish.name.length) {
                        if (badLast) dish.name.substring(start, end - 1)
                        else
                        // Returns a substring of this string that starts at the
                        // specified startIndex and continues to the end of the string
                            dish.name.substring(start)
                    } else {
                        // Returns the substring of this string starting at the
                        // startIndex and ending right before the endIndex
                        dish.name.substring(start, end)
                    }
                    // Мерджим с имеющейся в suggs парой с ключом shortName,
                    // добавив к значению единичку. Либо, если такой пары в
                    // suggs не было, то добавляем такую пару в мапу suggs
                    suggs.merge(shortName.lowercase(), 1) { i: Int, _: Int -> i + 1 }
                    // Если shortName лежит на правом краю имени блюда, то
                    // j == dish.name.length. Иначе - можно еще поискать вхождения
                }
            }
        Log.e("TAG", "findSuggestions: suggestions => $suggs")
        return suggs
    }

    override suspend fun addDishToCart(dishId: String) {
        val count = cartDao.dishCount(dishId) ?: 0
        // Если в корзине уже есть такое блюдо, то просто увеличиваем
        // на 1 количество штук этого блюда в корзине
        if (count > 0) cartDao.updateItemCount(dishId, count.inc())
        // Если в корзине такого блюда еще не было, то добавляем его
        else cartDao.addItem(CartItemPersist(dishId = dishId))
    }

    override suspend fun decrementOrRemoveDishFromCart(dishId: String) {
        val count = cartDao.dishCount(dishId) ?: 0
        // Когда в корзине такого блюда - 2 шт и более, то просто
        // делаем декремент количества штук этого блюда
        if (count > 1) cartDao.decrementItemCount(dishId)
        // Если такое блюдо в единственном числе, то удаляем его из корзины
        else cartDao.removeItem(dishId)
        // Здесь count не может быть 0, так как дать команду на удаление
        // юзер может только для уже добавленного в корзину блюда
    }

    override suspend fun cartCount(): Int = cartDao.cartCount() ?: 0
}