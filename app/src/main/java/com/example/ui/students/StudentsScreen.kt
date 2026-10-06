package com.example.ui.students

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.StudentEntity
import com.example.ui.MainViewModel
import com.example.ui.sync.GoogleSheetSyncDialog

@Composable
fun StudentsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val students by viewModel.filteredStudents.collectAsStateWithLifecycle()
    val allStudentsCount by viewModel.allStudents.collectAsStateWithLifecycle()
    val searchQuery by viewModel.studentSearchQuery.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showSyncDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var studentToEdit by remember { mutableStateOf<StudentEntity?>(null) }
    var studentToDelete by remember { mutableStateOf<StudentEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_student")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Student"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Student",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateStudentSearchQuery(it) },
                placeholder = { Text("Search by name, roll no, or room...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.updateStudentSearchQuery("") },
                            modifier = Modifier.testTag("btn_clear_student_search")
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_student_search")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Header summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enrolled Students (${allStudentsCount.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { showSyncDialog = true },
                        modifier = Modifier.testTag("btn_open_sheet_sync_students")
                    ) {
                        Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sheet Sync", fontSize = 12.sp)
                    }

                    if (allStudentsCount.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.testTag("btn_clear_all_students")
                        ) {
                            Text("Clear All", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (students.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching students found" else "No students enrolled yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try a different search term" else "Connect Google Sheet to import student list directly, or add manually with auto-roll generation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        if (allStudentsCount.isEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showSyncDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .testTag("btn_get_data_from_sheet_empty")
                                ) {
                                    Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sync with Google Sheet", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { showAddDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .testTag("btn_add_student_empty")
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add Student Manually (Auto Roll)")
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showAddDialog = true },
                                modifier = Modifier.testTag("btn_add_student_filtered_empty")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Student")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("student_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp)
                ) {
                    items(students, key = { it.id }) { student ->
                        StudentCard(
                            student = student,
                            onEdit = { studentToEdit = student },
                            onDelete = { studentToDelete = student }
                        )
                    }
                }
            }
        }
    }

    // Add Student Dialog with Auto-Generated Roll Number
    if (showAddDialog) {
        val nextRoll = remember { viewModel.getNextAutoRollNumber() }
        StudentFormDialog(
            title = "Add New Student",
            initialName = "",
            initialRollNo = nextRoll,
            initialRoomNo = "",
            initialPhone = "",
            isAutoGenerated = true,
            onAutoGenerateRoll = { viewModel.getNextAutoRollNumber() },
            onDismiss = { showAddDialog = false },
            onConfirm = { name, rollNo, roomNo, phone ->
                viewModel.addStudent(name, rollNo, roomNo, phone)
                showAddDialog = false
            }
        )
    }

    // Google Sheets Sync Dialog
    if (showSyncDialog) {
        GoogleSheetSyncDialog(
            viewModel = viewModel,
            onDismiss = { showSyncDialog = false }
        )
    }

    // Edit Student Dialog
    studentToEdit?.let { student ->
        StudentFormDialog(
            title = "Edit Student",
            initialName = student.name,
            initialRollNo = student.rollNo,
            initialRoomNo = student.roomNo,
            initialPhone = student.phone,
            onDismiss = { studentToEdit = null },
            onConfirm = { name, rollNo, roomNo, phone ->
                viewModel.updateStudent(
                    student.copy(
                        name = name,
                        rollNo = rollNo,
                        roomNo = roomNo,
                        phone = phone
                    )
                )
                studentToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    studentToDelete?.let { student ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("Delete Student") },
            text = { Text("Are you sure you want to remove ${student.name} (Roll #${student.rollNo})? All associated meal attendance records will also be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStudent(student)
                        studentToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("btn_confirm_delete_student")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { studentToDelete = null },
                    modifier = Modifier.testTag("btn_cancel_delete_student")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Students & Records?") },
            text = { Text("This will permanently remove all enrolled students and attendance records from local storage. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData {
                            showClearConfirm = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("btn_confirm_clear_all")
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirm = false },
                    modifier = Modifier.testTag("btn_cancel_clear_all")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StudentCard(
    student: StudentEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("student_card_${student.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Student Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Student Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = student.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "#${student.rollNo}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (student.roomNo.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MeetingRoom,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Room ${student.roomNo}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (student.phone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = student.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Edit & Delete Actions
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("btn_edit_student_${student.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Student",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("btn_delete_student_${student.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Student",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun StudentFormDialog(
    title: String,
    initialName: String,
    initialRollNo: String,
    initialRoomNo: String,
    initialPhone: String,
    isAutoGenerated: Boolean = false,
    onAutoGenerateRoll: (() -> String)? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, rollNo: String, roomNo: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var rollNo by remember { mutableStateOf(initialRollNo) }
    var roomNo by remember { mutableStateOf(initialRoomNo) }
    var phone by remember { mutableStateOf(initialPhone) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (errorText != null) errorText = null
                    },
                    label = { Text("Student Name *") },
                    placeholder = { Text("e.g. Rahul Sharma") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_student_name_input")
                )

                OutlinedTextField(
                    value = rollNo,
                    onValueChange = {
                        rollNo = it
                        if (errorText != null) errorText = null
                    },
                    label = { Text("Roll / Student ID Number *") },
                    placeholder = { Text("e.g. 101") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    trailingIcon = {
                        if (onAutoGenerateRoll != null) {
                            IconButton(
                                onClick = { rollNo = onAutoGenerateRoll() },
                                modifier = Modifier.testTag("btn_refresh_roll_number")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = "Regenerate Roll Number",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    supportingText = {
                        if (isAutoGenerated) {
                            Text(
                                text = "Auto-generated next sequence (editable)",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_student_roll_input")
                )

                OutlinedTextField(
                    value = roomNo,
                    onValueChange = { roomNo = it },
                    label = { Text("Room / Hostel No (Optional)") },
                    placeholder = { Text("e.g. A-204") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_student_room_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Contact Phone (Optional)") },
                    placeholder = { Text("e.g. 9876543210") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_student_phone_input")
                )

                errorText?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorText = "Please enter student name"
                        return@Button
                    }
                    if (rollNo.isBlank()) {
                        errorText = "Please enter roll / ID number"
                        return@Button
                    }
                    onConfirm(name.trim(), rollNo.trim(), roomNo.trim(), phone.trim())
                },
                modifier = Modifier.testTag("dialog_btn_save_student")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_btn_cancel_student")
            ) {
                Text("Cancel")
            }
        }
    )
}
