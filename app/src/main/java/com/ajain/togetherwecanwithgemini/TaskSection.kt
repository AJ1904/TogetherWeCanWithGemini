package com.ajain.togetherwecanwithgemini

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
    // Column to display a section of tasks with a title
    Column(
        modifier = Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Display the title of the section
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        // Iterate over the list of tasks and display each task item
        tasks.forEachIndexed { index, task ->
            TaskItem(task, index, expandedTaskIndex == index, sdgName) {
                // Expand or collapse the task item on click
                (if (expandedTaskIndex == index) null else index)?.let { onTaskExpand(it) }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun TaskItem(task: String, index: Int, expanded: Boolean, sdgName: String, onExpand: () -> Unit) {
    val context = LocalContext.current
    
    // Column to display a task item with click functionality
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpand() }
    ) {
        // Row for displaying the task content with expandable view
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Column for displaying the task text
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = if (expanded) task else task.take(50) + "...", // Show full or truncated text
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            // Icon to indicate expand/collapse action
            Icon(
                painter = painterResource(id = R.drawable.baseline_keyboard_arrow_right_24),
                contentDescription = stringResource(R.string.click_to_view_details),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        // Additional actions when the task is expanded
        if (expanded) {
            Row {
                Button(
                    onClick = {
                        // Save the goal to Firebase on button click
                        saveGoalToFirebase(
                            title = task.substringBefore(":").trim(),
                            description = task.substringAfter(":").trim(),
                            sdg = sdgName,
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
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary,
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.add_as_goal))
                }
            }
        }
    }
}
