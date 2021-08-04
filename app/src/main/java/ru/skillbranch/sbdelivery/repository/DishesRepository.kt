package ru.skillbranch.sbdelivery.repository

import android.util.Log
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
    suspend fun addDishToCart(id: String)
    suspend fun removeDishFromCart(dishId: String)
    suspend fun cartCount(): Int
}

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
        // Надо вернуть мапу из пар (ключ/значение), где
        // ключ - название блюда, содержащее в себе query-подстроку,
        // значение - количество блюд с таким названием в магазине
        val suggs = mutableMapOf<String, Int>()
        if (query.isBlank()) return suggs
        else dishesDao.findDishesFrom(query)
            .map { dish ->
                    // Индекс первого вхождения запроса в имя блюда
                    val idx = dish.name.indexOf(query.trim(), ignoreCase = true)
                    // Сдвигаем индекс на следующий после запроса символ
                    var j = idx + query.trim().length
                    while (j < dish.name.length) {
                        // Ищем индекс символа, оканчивающего слово
                        if (dish.name[j] == ' ') break
                        j++
                    }
                    val shortName = dish.name.substring(0, j)
                    // Мерджим блюда с одинакомым shortName в одну запись
                    // и учитываем их количество
                    suggs.merge(shortName, 1) { i: Int, _: Int -> i + 1 }
            }
        Log.e("TAG", "findSuggestions: suggestions => $suggs")
        return suggs
    }

    override suspend fun addDishToCart(id: String) {
        val count = cartDao.dishCount(id) ?: 0
        if (count > 0) cartDao.updateItemCount(id, count.inc())
        else cartDao.addItem(CartItemPersist(dishId = id))
    }

    override suspend fun removeDishFromCart(id: String) {
        val count = cartDao.dishCount(id) ?: 0
        if (count > 0) cartDao.decrementItemCount(id)
        else cartDao.removeItem(id)
    }

    override suspend fun cartCount(): Int = cartDao.cartCount() ?: 0
}