package com.marcdonald.hibi.screens.searchentries.searchentriesscreen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcdonald.hibi.data.entity.Book
import com.marcdonald.hibi.data.entity.Entry
import com.marcdonald.hibi.data.entity.Tag
import com.marcdonald.hibi.data.repository.*
import com.marcdonald.hibi.internal.utils.formatDateForDisplay
import com.marcdonald.hibi.screens.mainentriesrecycler.BookEntryDisplayItem
import com.marcdonald.hibi.screens.mainentriesrecycler.MainEntriesDisplayItem
import com.marcdonald.hibi.screens.mainentriesrecycler.TagEntryDisplayItem
import com.marcdonald.hibi.screens.searchentries.EntrySearchCriteria
import kotlinx.coroutines.launch

class SearchEntriesViewModel(private val entryRepository: EntryRepository,
														 private val tagRepository: TagRepository,
														 private val tagEntryRelationRepository: TagEntryRelationRepository,
														 private val bookRepository: BookRepository,
														 private val bookEntryRelationRepository: BookEntryRelationRepository)
	: ViewModel() {

	private val _criteria = EntrySearchCriteria()

	// <editor-fold desc="Main Search Properties">
	private val _entries = MutableLiveData<List<MainEntriesDisplayItem>>()
	val entries: LiveData<List<MainEntriesDisplayItem>>
		get() = _entries

	private val _displayLoading = MutableLiveData<Boolean>()
	val displayLoading: LiveData<Boolean>
		get() = _displayLoading

	private val _displayNoResults = MutableLiveData<Boolean>()
	val displayNoResults: LiveData<Boolean>
		get() = _displayNoResults

	private val _countResults = MutableLiveData<Int>()
	val countResults: LiveData<Int>
		get() = _countResults
	// </editor-fold>

	// <editor-fold desc="Bottom Sheet Properties">
	private val _beginningDisplay = MutableLiveData<String>()
	val beginningDisplay: LiveData<String>
		get() = _beginningDisplay

	val startDay: Int
		get() = _criteria.startDay
	val startMonth: Int
		get() = _criteria.startMonth
	val startYear: Int
		get() = _criteria.startYear

	val endDay: Int
		get() = _criteria.endDay
	val endMonth: Int
		get() = _criteria.endMonth
	val endYear: Int
		get() = _criteria.endYear

	private val _endDisplay = MutableLiveData<String>()
	val endDisplay: LiveData<String>
		get() = _endDisplay

	private val _containingDisplay = MutableLiveData<String>()
	val containingDisplay: LiveData<String>
		get() = _containingDisplay

	private val _locationDisplay = MutableLiveData<String>()
	val locationDisplay: LiveData<String>
		get() = _locationDisplay

	private val _dismissBottomSheet = MutableLiveData<Boolean>()
	val dismissBottomSheet: LiveData<Boolean>
		get() = _dismissBottomSheet

	private val _clearDisplays = MutableLiveData<Boolean>()
	val clearChipTicks: LiveData<Boolean>
		get() = _clearDisplays

	private val _tags = MutableLiveData<List<Tag>>()
	val tags: LiveData<List<Tag>>
		get() = _tags

	private val _checkedTags = MutableLiveData<List<Int>>()
	val checkedTags: LiveData<List<Int>>
		get() = _checkedTags

	private val _checkedBooks = MutableLiveData<List<Int>>()
	val checkedBooks: LiveData<List<Int>>
		get() = _checkedBooks

	private val _displayNoTagsWarning = MutableLiveData<Boolean>()
	val displayNoTagsWarning: LiveData<Boolean>
		get() = _displayNoTagsWarning

	private val _books = MutableLiveData<List<Book>>()
	val books: LiveData<List<Book>>
		get() = _books

	private val _displayNoBooksWarning = MutableLiveData<Boolean>()
	val displayNoBooksWarning: LiveData<Boolean>
		get() = _displayNoBooksWarning
	// </editor-fold>

	init {
		_displayLoading.value = false
		getTags()
		getBooks()
		_locationDisplay.value = _criteria.location
		_containingDisplay.value = _criteria.content
	}

	private fun onCriteriaChange() {
		viewModelScope.launch {
			_displayLoading.value = true
			_displayNoResults.value = false
			displayResults(_criteria)
			_checkedTags.value = _criteria.tags
			_checkedBooks.value = _criteria.books
			_displayLoading.value = false
			_displayNoResults.value = entries.value == null || entries.value!!.isEmpty()
		}
	}

	// <editor-fold desc="Filters">
	private suspend fun filterEntries(searchCriteria: EntrySearchCriteria): List<Entry> {
		val allEntries = entryRepository.getAllEntries()
		val filteredByDate = filterByDate(allEntries, searchCriteria)
		val filteredByContent = filterByContent(filteredByDate, searchCriteria)
		val filteredByLocation = filterByLocation(filteredByContent, searchCriteria)
		val filteredByTag = filterByTag(filteredByLocation, searchCriteria)
		return filterByBook(filteredByTag, searchCriteria)
	}

	private fun filterByDate(entries: List<Entry>, searchCriteria: EntrySearchCriteria): List<Entry> {
		val returnList = mutableListOf<Entry>()
		val completeStartDate = getCompleteDate(searchCriteria.startDay, searchCriteria.startMonth, searchCriteria.startYear)
		val completeEndDate = getCompleteDate(searchCriteria.endDay, searchCriteria.endMonth, searchCriteria.endYear)

		for(entry in entries) {
			val entryCompleteDate = getCompleteDate(entry.day, entry.month, entry.year)
			if(entryCompleteDate in completeStartDate..completeEndDate)
				returnList.add(entry)
		}
		return returnList
	}

	private fun filterByContent(entries: List<Entry>, searchCriteria: EntrySearchCriteria): List<Entry> {
		val returnList = mutableListOf<Entry>()

		if(searchCriteria.content.isNotBlank()) {
			for(entry in entries) {
				if(entry.content.contains(searchCriteria.content, true))
					returnList.add(entry)
			}
		} else {
			return entries
		}

		return returnList
	}

	private fun filterByLocation(entries: List<Entry>, searchCriteria: EntrySearchCriteria): List<Entry> {
		val returnList = mutableListOf<Entry>()

		if(searchCriteria.location.isNotBlank()) {
			for(entry in entries) {
				if(entry.location.contains(searchCriteria.location, true))
					returnList.add(entry)
			}
		} else {
			return entries
		}

		return returnList
	}

	private suspend fun filterByTag(entries: List<Entry>, searchCriteria: EntrySearchCriteria): List<Entry> {
		val returnList = mutableListOf<Entry>()
		val tagEntryRelations = tagEntryRelationRepository.getAllTagEntryRelationsWithIds(searchCriteria.tags)

		if(searchCriteria.tags.isNotEmpty()) {
			entries.forEach { entry ->
				tagEntryRelations.forEach { tagEntryRelation ->
					if(tagEntryRelation.entryId == entry.id && !returnList.contains(entry))
						returnList.add(entry)
				}
			}
		} else {
			return entries
		}
		return returnList
	}

	private suspend fun filterByBook(entries: List<Entry>, searchCriteria: EntrySearchCriteria): List<Entry> {
		val returnList = mutableListOf<Entry>()
		val bookEntryRelations = bookEntryRelationRepository.getAllBookEntryRelationsWithIds(searchCriteria.books)

		if(searchCriteria.books.isNotEmpty()) {
			entries.forEach { entry ->
				bookEntryRelations.forEach { bookEntryRelation ->
					if(bookEntryRelation.entryId == entry.id && !returnList.contains(entry))
						returnList.add(entry)
				}
			}
		} else {
			return entries
		}
		return returnList
	}
	// </editor-fold>

	// <editor-fold desc="Result Display Methods">
	private suspend fun displayResults(searchCriteria: EntrySearchCriteria) {
		val filteredResults = filterEntries(searchCriteria)
		val tagEntryDisplayItems = tagEntryRelationRepository.getTagEntryDisplayItems()
		val bookEntryDisplayItems = bookEntryRelationRepository.getBookEntryDisplayItems()
		_entries.value = combineData(filteredResults, tagEntryDisplayItems, bookEntryDisplayItems)
		_countResults.value = filteredResults.size
	}

	private fun combineData(entries: List<Entry>, tagEntryDisplayItems: List<TagEntryDisplayItem>, bookEntryDisplayItems: List<BookEntryDisplayItem>): List<MainEntriesDisplayItem> {
		val itemList = ArrayList<MainEntriesDisplayItem>()

		entries.forEach { entry ->
			val item = MainEntriesDisplayItem(entry, listOf(), listOf())
			val listOfTags = ArrayList<String>()
			val listOfBooks = ArrayList<String>()

			tagEntryDisplayItems.forEach { tagEntryDisplayItem ->
				if(tagEntryDisplayItem.entryId == entry.id) {
					listOfTags.add(tagEntryDisplayItem.tagName)
				}
			}

			bookEntryDisplayItems.forEach { bookEntryDisplayItem ->
				if(bookEntryDisplayItem.entryId == entry.id) {
					listOfBooks.add(bookEntryDisplayItem.bookName)
				}
			}

			item.tags = listOfTags
			item.books = listOfBooks
			itemList.add(item)
		}

		return addListHeaders(itemList)
	}

	private fun addListHeaders(allItems: MutableList<MainEntriesDisplayItem>): List<MainEntriesDisplayItem> {
		val listWithHeaders = mutableListOf<MainEntriesDisplayItem>()
		listWithHeaders.addAll(allItems)

		var lastMonth = 12
		var lastYear = 9999
		if(allItems.isNotEmpty()) {
			lastMonth = allItems.first().entry.month + 1
			lastYear = allItems.first().entry.year
		}

		val headersToAdd = mutableListOf<Pair<Int, MainEntriesDisplayItem>>()

		for(x in 0 until allItems.size) {
			if(((allItems[x].entry.month < lastMonth) && (allItems[x].entry.year == lastYear))
				 || (allItems[x].entry.month > lastMonth) && (allItems[x].entry.year < lastYear)
				 || (allItems[x].entry.year < lastYear)
			) {
				val header = Entry(0, allItems[x].entry.month, allItems[x].entry.year, 0, 0, "")
				val headerItem = MainEntriesDisplayItem(header, listOf(), listOf())
				lastMonth = allItems[x].entry.month
				lastYear = allItems[x].entry.year
				headersToAdd.add(Pair(x, headerItem))
			}
		}

		for((add, x) in (0 until headersToAdd.size).withIndex()) {
			listWithHeaders.add(headersToAdd[x].first + add, headersToAdd[x].second)
		}

		return listWithHeaders
	}
	// </editor-fold>

	// <editor-fold desc="Bottom Sheet">
	fun reset() {
		resetStartDate()
		resetEndDate()
		resetContaining()
		resetLocation()
		_criteria.tags = listOf()
		_criteria.books = listOf()
		_checkedBooks.value = listOf()
		_checkedTags.value = listOf()
		_clearDisplays.value = true
	}

	fun setStartDate(year: Int, month: Int, day: Int) {
		_criteria.startYear = year
		_criteria.startMonth = month
		_criteria.startDay = day
		_beginningDisplay.value = formatDateForDisplay(day, month, year)
	}

	fun startIsBeginning(): Boolean {
		return (_criteria.startYear == 0 && _criteria.startDay == 1 && _criteria.startMonth == 0)
	}

	fun endIsFinish(): Boolean {
		return (_criteria.endYear == 9999 && _criteria.endDay == 31 && _criteria.endMonth == 11)
	}

	fun setEndDate(year: Int, month: Int, day: Int) {
		_criteria.endYear = year
		_criteria.endMonth = month
		_criteria.endDay = day
		_endDisplay.value = formatDateForDisplay(day, month, year)
	}

	fun setContaining(contentArg: String) {
		_criteria.content = contentArg
		_containingDisplay.value = contentArg
	}

	fun setLocation(locationArg: String) {
		_criteria.location = locationArg
		_locationDisplay.value = locationArg
	}

	fun search(tagsArg: List<Int>, booksArg: List<Int>) {
		_criteria.tags = tagsArg
		_criteria.books = booksArg
		onCriteriaChange()
		_dismissBottomSheet.value = true
	}

	fun resetStartDate() {
		_criteria.startYear = 0
		_criteria.startDay = 1
		_criteria.startMonth = 0
		_beginningDisplay.value = ""
	}

	fun resetEndDate() {
		_criteria.endYear = 9999
		_criteria.endDay = 31
		_criteria.endMonth = 11
		_endDisplay.value = ""
	}

	fun resetContaining() {
		_criteria.content = ""
		_containingDisplay.value = _criteria.content
	}

	fun resetLocation() {
		_criteria.location = ""
		_locationDisplay.value = _criteria.location
	}

	private fun getTags() {
		viewModelScope.launch {
			val allTags = tagRepository.getAllTags()
			_tags.value = allTags
			_displayNoTagsWarning.value = allTags.isEmpty()
		}
	}

	private fun getBooks() {
		viewModelScope.launch {
			val allBooks = bookRepository.getAllBooks()
			_books.value = allBooks
			_displayNoBooksWarning.value = allBooks.isEmpty()
		}
	}
	// </editor-fold>

	private fun getCompleteDate(day: Int, month: Int, year: Int): Int {
		val dayTwoDigit = if(day < 10)
			"0$day"
		else {
			"$day"
		}

		val monthTwoDigit = if(month < 10)
			"0$month"
		else {
			"$month"
		}

		return "$year$monthTwoDigit$dayTwoDigit".toInt()
	}
}