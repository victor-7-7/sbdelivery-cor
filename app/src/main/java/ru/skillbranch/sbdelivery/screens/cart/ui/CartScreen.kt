package ru.skillbranch.sbdelivery.screens.cart.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.skillbranch.sbdelivery.aop.LogAspect
import ru.skillbranch.sbdelivery.screens.cart.logic.CartFeature
import ru.skillbranch.sbdelivery.screens.cart.data.CartUiState
import ru.skillbranch.sbdelivery.screens.cart.data.ConfirmDialogState

@Composable
fun CartScreen(state: CartFeature.State, accept: (CartFeature.Msg) -> Unit) {
    Log.w(LogAspect.tag, ">>>--------CartScreen() Params: [state = $state]")
    when (state.uiState) {
        is CartUiState.Things -> {
            Column {
                LazyColumn(
                    contentPadding = PaddingValues(0.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = {
                        val items = state.uiState.dishes
                        items(items = items, key = { it.id }) {
                            CartListItem(
                                dish = it,
                                onProductClick = { dishId, title ->
                                    accept(CartFeature.Msg.ClickOnDish(dishId, title))
                                },
                                onIncrement = { dishId ->
                                    accept(CartFeature.Msg.IncrementCount(dishId))
                                },
                                onDecrement = { dishId ->
                                    accept(CartFeature.Msg.DecrementCount(dishId))
                                },
                                onRemove = { dishId, title ->
                                    Log.w(LogAspect.tag, "CartListItem.onRemove() click")
                                    // ???????????????????? ?????????? ?????????????????????? ???????????????? ???? ??????????????
                                    accept(CartFeature.Msg.ShowConfirm(dishId, title))
                                }
                            )
                        }

                    },
                    modifier = Modifier.weight(1f)
                )

                Column(
                    modifier = Modifier
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row {
                        val total = state.uiState.dishes.sumOf { it.count * it.price }
                        Text(
                            "??????????",
                            fontSize = 24.sp,
                            style = TextStyle(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colors.onPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "$total ??",
                            fontSize = 24.sp,
                            style = TextStyle(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colors.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val order = mutableMapOf<String, Int>()
                            state.uiState.dishes.forEach { dish ->
                                order[dish.id] = dish.count
                            }
                            accept(CartFeature.Msg.SendOrder(order))
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.secondary,
                            contentColor = MaterialTheme.colors.onSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("???????????????? ??????????", style = TextStyle(fontWeight = FontWeight.Bold))
                    }
                }
            }

        }
        is CartUiState.Empty -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(text = "???????? ???????????? ??????")
        }

        is CartUiState.Loading -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CircularProgressIndicator(color = MaterialTheme.colors.secondary)
        }
    }

    if (state.confirmDialog is ConfirmDialogState.Show) {
        Log.w(LogAspect.tag, ">>>--------Show Alert Dialog: [dishId = ${state.confirmDialog.id}] [dishTitle = ${state.confirmDialog.title}]")
        AlertDialog(
            // onDismissRequest -> Executes when the user tries to dismiss the Dialog
            // by clicking outside or pressing the back button. This is not called
            // when the dismiss button is clicked
            onDismissRequest = {  accept(CartFeature.Msg.HideConfirm)  },
            backgroundColor = Color.White,
            contentColor = MaterialTheme.colors.primary,
            title = { Text(text = "???? ???????????????") },
            text = { Text(text = "???? ?????????? ???????????? ?????????????? ${state.confirmDialog.title} ???? ??????????????") },
            buttons = {
                Row {
                    TextButton(
                        onClick = { accept(CartFeature.Msg.HideConfirm) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("??????", color = MaterialTheme.colors.secondary)
                    }
                    TextButton(
                        onClick = {
                            accept(CartFeature.Msg.RemoveFromCart(state.confirmDialog.id))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("????", color = MaterialTheme.colors.secondary)
                    }
                }

            }
        )
    }
    Log.w(LogAspect.tag, "<<<--------CartScreen()")
}