package com.example.ca3

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.*
import com.example.ca3.ui.theme.CA3Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CA3Theme {
                ContactScreen()
            }
        }
    }
}

@Entity(tableName = "contact_book")
data class ContactBook(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val phnum: String
)

@Dao
interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContact(contact: ContactBook): Long

    @Query("SELECT * FROM contact_book")
    fun getAllContacts(): Flow<List<ContactBook>>

    @Query("SELECT COUNT(*) FROM contact_book")
    suspend fun countContacts(): Int
}

@Database(
    entities = [ContactBook::class],
    version = 1,
    exportSchema = false
)
abstract class ContactDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    companion object {
        @Volatile
        private var INSTANCE: ContactDatabase? = null
        fun getDatabase(context: Application): ContactDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context,
                    ContactDatabase::class.java,
                    "contact_book_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}

class ContactViewModel(application: Application) :
    AndroidViewModel(application) {

    private val dao =
        ContactDatabase.getDatabase(application).contactDao()

    val contacts = dao.getAllContacts()

    var message by mutableStateOf("")
        private set

    fun addContact(name: String, phone: String) {

        viewModelScope.launch {
            val count = dao.countContacts()

            if (count >= 3) {
                message = "Limit reached! Only 3 contacts allowed."
            }else {
                dao.insertContact(
                    ContactBook(
                        name = name,
                        phnum = phone
                    )
                )
                message = "Contact added successfully!"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(
    viewModel: ContactViewModel = viewModel()
) {
    val contacts by viewModel.contacts.collectAsState(
        initial = emptyList()
    )

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Contact Book") })
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Enter Name") }
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Enter Phone Number") }
            )

            Button(
                onClick = {
                    viewModel.addContact(name, phone)
                    name = ""
                    phone = ""
                }
            ) {
                Text("Add Contact")
            }

            Text(viewModel.message)

            Text("Saved Contacts (${contacts.size}/3)")

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(contacts) { contact ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text("Name: ${contact.name}")
                            Text("Phone: ${contact.phnum}")
                        }
                    }
                }
            }
        }
    }
}
