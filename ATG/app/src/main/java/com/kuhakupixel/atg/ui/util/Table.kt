package com.kuhakupixel.atg.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontVariation.width
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.nanihadesuka.compose.LazyColumnScrollbar
import kotlin.math.max

@Composable
fun CreateTable(
    modifier: Modifier = Modifier,
    colNames: List<String>,
    colWeights: List<Float>,
    itemCount: Int,
    minEmptyItemCount: Int = 0,
    rowMinHeight: Dp = 0.dp,
    onRowClicked: (rowIndex: Int) -> Unit,
    drawCell: @Composable (rowIndex: Int, colIndex: Int) -> Unit,

    ) {
    @Composable
    fun RowScope.GetCellModifier(
        weight: Float
    ): Modifier {
        return Modifier
            .border(1.dp, MaterialTheme.colorScheme.primary)
            .weight(weight = weight)
            .padding(8.dp)
            // Remove horizontal scroll to allow text wrapping
            // .horizontalScroll(rememberScrollState())
            // so children's size in [drawCell] can be as big as possible
            // https://stackoverflow.com/questions/67677125/fill-height-for-child-in-row
            // https://stackoverflow.com/questions/65942711/match-width-of-parent-in-column-jetpack-compose
            .width(IntrinsicSize.Max)
    }

    @Composable
    fun RowScope.TableCell(
        text: String,
        weight: Float
    ) {
        Text(
            text = text,
            modifier = GetCellModifier(weight).defaultMinSize(minHeight = rowMinHeight),
            // Allow text to wrap to multiple lines for better readability
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Visible
        )
    }
    if (colNames.size != colWeights.size) {
        throw IllegalArgumentException("Column names and weights length not equal")
    }
    if (colNames.isEmpty()) {
        throw IllegalArgumentException("Column names empty")
    }

    val colCount: Int = colNames.size

    Column(modifier = modifier) {
        // header
        Row(Modifier.background(MaterialTheme.colorScheme.primaryContainer)) {
            for (i in 0 until colNames.size)
                TableCell(text = colNames[i], weight = colWeights[i])
        }
        val lazyColumnState = rememberLazyListState()
        // items
        LazyColumnScrollbar(listState = lazyColumnState) {
            LazyColumn(state = lazyColumnState) {
                // items
                items(itemCount) { rowIndex: Int ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // when row is clicked
                            .clickable {
                                onRowClicked(rowIndex)
                            },
                    ) {

                        for (colIndex in 0 until colCount) {
                            Box(
                                modifier = GetCellModifier(colWeights[colIndex]).defaultMinSize(
                                    minHeight = rowMinHeight
                                )
                            ) {
                                drawCell(
                                    rowIndex = rowIndex,
                                    colIndex = colIndex,
                                )
                            }
                        }
                    }
                }
                // ============== show Empty Item ======================
                var emptyItemShownCount: Int = max(0, minEmptyItemCount - itemCount)
                items(emptyItemShownCount) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {}
                    ) {
                        for (i in 0 until colCount)
                            TableCell(text = "", weight = colWeights[i])

                    }
                }
            }

        }
    }
}