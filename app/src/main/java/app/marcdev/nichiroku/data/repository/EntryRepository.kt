package app.marcdev.nichiroku.data.repository

import androidx.lifecycle.LiveData
import app.marcdev.nichiroku.data.entity.Entry

interface EntryRepository {

  suspend fun addEntry(entry: Entry)

  suspend fun getEntry(id: Int): LiveData<Entry>

  suspend fun getAllEntries(): LiveData<List<Entry>>

  suspend fun deleteEntry(id: Int)

  suspend fun getAmountOfEntries(): LiveData<Int>
}