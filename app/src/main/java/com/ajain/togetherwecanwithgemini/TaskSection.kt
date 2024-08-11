package com.ajain.togetherwecanwithgemini

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.FlowRowScopeInstance.align
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun TaskSection(
    title: String,
    tasks: List<String>,
    color: Color,
    expandedTaskIndex: Int?,
    sdgName: String,
    onTaskExpand: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            //.background(color = color)
           // .background(color = MaterialTheme.colorScheme.background)
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        Text(text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        tasks.forEachIndexed { index, task ->
            TaskItem(task, index, expandedTaskIndex == index, sdgName) {
                (if (expandedTaskIndex == index) null else index)?.let { onTaskExpand(it) }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun TaskItem(task: String, index: Int, expanded: Boolean, sdgName: String, onExpand: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable { onExpand() }
       // .padding(8.dp)

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically // Center items vertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = if (expanded) task else task.take(50) + "...",
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    color = MaterialTheme.colorScheme.onPrimary
                )

            }
            Icon(
                painter = painterResource(id = R.drawable.baseline_keyboard_arrow_right_24),
                contentDescription = stringResource(R.string.click_to_view_details),
                tint = MaterialTheme.colorScheme.onPrimary
            )


        }
        if (expanded) {
            Row {
                Button(onClick = {
                    saveGoalToFirebase(
                        title = task.substringBefore(":").trim(),
                        description = task.substringAfter(":").trim(),
                        sdg = sdgName,  // Replace with actual SDG title or ID
                        onSuccess = {
                            Toast.makeText(
                                context,
                                context.getString(R.string.goal_added_successfully),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onFailure = { exception ->
                            Toast.makeText(
                                context,
                                context.getString(R.string.failed_to_add_goal) + exception.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                },
                    colors = ButtonDefaults.buttonColors(
                        containerColor= MaterialTheme.colorScheme.tertiary,
                        contentColor= MaterialTheme.colorScheme.onTertiary,
                        disabledContainerColor= MaterialTheme.colorScheme.primary,
                        disabledContentColor= MaterialTheme.colorScheme.onPrimary
                    )) {
                    Text(stringResource(R.string.add_as_goal))
                }

            }
        }
    }
}