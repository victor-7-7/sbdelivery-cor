package ru.skillbranch.sbdelivery.screens.dish.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester.Companion.createRefs
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.rememberImagePainter
import ru.skillbranch.sbdelivery.R
import ru.skillbranch.sbdelivery.screens.dish.logic.DishFeature
import ru.skillbranch.sbdelivery.screens.dish.data.DishContent
import ru.skillbranch.sbdelivery.screens.root.ui.AppTheme

@Composable
fun DishContent(dish: DishContent, count: Int, isLiked: Boolean, accept: (DishFeature.Msg) -> Unit) {
    ConstraintLayout {

        val (title, poster, description, price, addBtn) = createRefs()

        // https://google.github.io/accompanist/coil/
        val painter = rememberImagePainter(
                data = dish.image,
                builder = {
                    crossfade(true)
                    placeholder(R.drawable.img_empty_place_holder)
            })

        Image(
            painter = painter,
            contentDescription = "My content description",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .aspectRatio(1.44f)
                .fillMaxSize()
                .constrainAs(poster) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        Text(
            fontSize = 24.sp,
            color = MaterialTheme.colors.onPrimary,
            style = TextStyle(fontWeight = FontWeight.Bold),
            text = dish.title,
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(title) {
                    top.linkTo(poster.bottom, margin = 16.dp)
                    start.linkTo(parent.start, margin = 16.dp)
                    end.linkTo(parent.end, margin = 16.dp)
                    width = Dimension.preferredWrapContent
                }

        )

        Text(
            fontSize = 14.sp,
            color = MaterialTheme.colors.onBackground,
            text = dish.description,
            style = TextStyle(fontWeight = FontWeight.ExtraLight),
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(description) {
                    top.linkTo(title.bottom, margin = 16.dp)
                    start.linkTo(parent.start, margin = 16.dp)
                    end.linkTo(parent.end, margin = 16.dp)
                    width = Dimension.preferredWrapContent
                }
        )

        DishPrice(price = dish.price, oldPrice = dish.oldPrice,
            count = count,
            onIncrement = { accept(DishFeature.Msg.IncrementCount) },
            onDecrement = { accept(DishFeature.Msg.DecrementCount) },
            accept = accept,
            isLiked = isLiked,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp)
                .constrainAs(price) {
                    top.linkTo(description.bottom, margin = 32.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                })

        TextButton(
            onClick = { accept(DishFeature.Msg.AddToCart(dish.id, count)) },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.secondary,
                contentColor = MaterialTheme.colors.onSecondary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .constrainAs(addBtn) {
                    top.linkTo(price.bottom, margin = 32.dp)
                    start.linkTo(parent.start, margin = 16.dp)
                    end.linkTo(parent.end, margin = 16.dp)
                    bottom.linkTo(parent.bottom, margin = 16.dp)
                    width = Dimension.preferredWrapContent
                }
        ) {
            Text(
                "???????????????? ?? ??????????????${if (count > 1) " ($count)" else ""}",
                style = TextStyle(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun Stepper(
    value: Int,
    modifier: Modifier = Modifier,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .border(
                0.dp,
                MaterialTheme.colors.onBackground,
                shape = RoundedCornerShape(4.dp)
            )
            .clip(RoundedCornerShape(4.dp))
            .animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (value > 1) {
            IconButton(
                onClick = { onDecrement() },
                content = {
                    Icon(
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colors.secondary,
                        painter = painterResource(R.drawable.ic_baseline_remove_24),
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .width(30.dp)
                    .fillMaxHeight()
                    .border(
                        0.dp,
                        MaterialTheme.colors.onBackground
                    )
                    .clipToBounds()
            )
        }

        Text(
            text = "$value",
            fontSize = 24.sp,
            style = TextStyle(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colors.secondary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp)
        )
        IconButton(
            onClick = { onIncrement() },
            content = {
                Icon(
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colors.secondary,
                    painter = painterResource(R.drawable.ic_baseline_add_24),
                    contentDescription = null
                )
            },
            modifier = Modifier
                .width(30.dp)
                .fillMaxHeight()
                .border(
                    0.dp,
                    MaterialTheme.colors.onBackground
                )
                .clipToBounds()
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DishPrice(
    price: Int,
    modifier: Modifier = Modifier,
    count: Int = 1, // ???????????????????? ???????? ??????????, ?????????????????????? ?? ?????????????? ???? ??????
    oldPrice: Int? = null,
    fontSize: Int = 24,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    accept: (DishFeature.Msg) -> Unit,
    isLiked: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier)
    {
        if (oldPrice != null) {
            Text(
                text = "${oldPrice * count} ??",
                color = MaterialTheme.colors.onPrimary,
                textDecoration = TextDecoration.LineThrough,
                style = TextStyle(fontWeight = FontWeight.ExtraLight),
                fontSize = fontSize.sp
            )
            Spacer(modifier = Modifier.width(8.dp))

        }

        Text(
            text = "${price * count} ??",
            color = MaterialTheme.colors.secondary,
            style = TextStyle(fontWeight = FontWeight.Bold),
            fontSize = fontSize.sp
        )

        IconButton(
            onClick = { accept(DishFeature.Msg.ToggleLike) },
            content = {
                Icon(
                    modifier = Modifier.size(36.dp).clipToBounds(),
                    tint = MaterialTheme.colors.secondary,
                    painter = if (isLiked) {
                        painterResource(R.drawable.ic_favorite_black_24dp)
                    } else {
                        painterResource(R.drawable.ic_favorite_border_black_24dp)
                    },
                    contentDescription = null
                )
            },
            modifier = Modifier
                .size(30.dp)
                .weight(1f)
        )

        Stepper(value = count, onIncrement = onIncrement, onDecrement = onDecrement)
    }
}

@Preview
@Composable
fun StepperPreview() {
    AppTheme {
        Stepper(10, onDecrement = {}, onIncrement = {})
    }
}

@Preview
@Composable
fun PricePreview() {
    AppTheme {
        DishPrice(60, oldPrice =100, count=5, onDecrement = {}, onIncrement = {}, accept = {})
    }
}

@Preview
@Composable
fun ContentPreview() {
    AppTheme {
        DishContent(
            dish = DishContent(
                "0",
                "https://www.delivery-club.ru/media/cms/relation_product/32350/312372888_m650.jpg",
                "???????????? \"??????????????\"",
                "320 ?? ??? ?????????????? ???? 100% ???????????????? (???????????????? medium) ???? ??????????, ???????????????????????? ?????????????? ???? ??????????, ?????????????????? ????????, ???????? ????????????, ??????????, ???????????????????????? ??????, ?????????????? ??????????, ?????? ????????????.",
                100,
                200
            ),
            count = 5,
            isLiked = false
        ) {}
    }
}

