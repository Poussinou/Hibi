package app.marcdev.hibi.maintabs.searchentries.searchentriesscreen

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.marcdev.hibi.R
import app.marcdev.hibi.internal.PREF_ENTRY_DIVIDERS
import app.marcdev.hibi.maintabs.mainentriesrecycler.EntriesRecyclerAdapter
import app.marcdev.hibi.uicomponents.DatePickerDialog
import app.marcdev.hibi.uicomponents.views.ChipWithId
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import timber.log.Timber

class SearchEntriesFragment : Fragment(), KodeinAware {
  override val kodein by closestKodein()

  // <editor-fold desc="View Model">
  private val viewModelFactory: SearchEntriesViewModelFactory by instance()
  private lateinit var viewModel: SearchEntriesViewModel
  // </editor-fold>

  // <editor-fold desc="UI Components">
  private lateinit var loadingDisplay: ConstraintLayout
  private lateinit var noResults: ConstraintLayout
  private lateinit var toolbarTitle: TextView
  private lateinit var recycler: RecyclerView
  private lateinit var recyclerAdapter: EntriesRecyclerAdapter
  private lateinit var criteriaBottomSheetRelativeLayout: RelativeLayout
  private lateinit var criteriaBottomSheetBehavior: BottomSheetBehavior<RelativeLayout>
  // </editor-fold>

  // <editor-fold desc="Bottom Sheet UI Components">
  private lateinit var beginningDateButton: MaterialButton
  private lateinit var endDateButton: MaterialButton
  private lateinit var contentInput: EditText
  private lateinit var locationInput: EditText
  private lateinit var tagChipGroup: ChipGroup
  private lateinit var noTagsWarning: TextView
  private lateinit var bookChipGroup: ChipGroup
  private lateinit var noBooksWarning: TextView
  private lateinit var searchButton: MaterialButton
  private lateinit var resetButton: MaterialButton
  private lateinit var startDateDialog: DatePickerDialog
  private lateinit var endDateDialog: DatePickerDialog
  // </editor-fold>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = ViewModelProviders.of(this, viewModelFactory).get(SearchEntriesViewModel::class.java)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    Timber.v("Log: onCreateView: Started")
    val view = inflater.inflate(R.layout.fragment_search_entries, container, false)

    bindViews(view)
    initRecycler(view)
    setupMainObservers()
    setupBottomSheetObservers()

    return view
  }

  private fun bindViews(view: View) {
    criteriaBottomSheetRelativeLayout = view.findViewById(R.id.bottom_sheet_search_entries)
    criteriaBottomSheetBehavior = BottomSheetBehavior.from(criteriaBottomSheetRelativeLayout)
    loadingDisplay = view.findViewById(R.id.const_search_entries_loading)
    noResults = view.findViewById(R.id.const_no_search_entries_results)
    toolbarTitle = view.findViewById(R.id.txt_back_toolbar_title)
    toolbarTitle.text = resources.getString(R.string.search_entries)
    val toolbarBack: ImageView = view.findViewById(R.id.img_back_toolbar_back)
    toolbarBack.setOnClickListener {
      Navigation.findNavController(requireView()).popBackStack()
    }
    initBottomSheet(view)
  }

  private fun setupMainObservers() {
    viewModel.entries.observe(this, Observer { value ->
      value?.let { list ->
        recyclerAdapter.updateList(list)
      }
    })

    viewModel.displayLoading.observe(this, Observer { value ->
      value?.let { show ->
        loadingDisplay.visibility = if(show) View.VISIBLE else View.GONE
      }
    })

    viewModel.countResults.observe(this, Observer { value ->
      value?.let { amount ->
        toolbarTitle.text = resources.getQuantityString(R.plurals.count_results, amount, amount)
      }
    })

    viewModel.displayNoResults.observe(this, Observer { value ->
      value?.let { show ->
        if(show) {
          noResults.visibility = View.VISIBLE
          recycler.visibility = View.GONE
        } else {
          noResults.visibility = View.GONE
          recycler.visibility = View.VISIBLE
        }
      }
    })
  }

  private fun initRecycler(view: View) {
    recycler = view.findViewById(R.id.recycler_search_entries)
    recyclerAdapter = EntriesRecyclerAdapter(requireContext())
    val layoutManager = LinearLayoutManager(context)
    recycler.adapter = recyclerAdapter
    recycler.layoutManager = layoutManager

    val includeEntryDividers = PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean(PREF_ENTRY_DIVIDERS, true)
    if(includeEntryDividers) {
      val dividerItemDecoration = DividerItemDecoration(recycler.context, layoutManager.orientation)
      recycler.addItemDecoration(dividerItemDecoration)
    }
  }

  private fun initBottomSheet(view: View) {
    initDateDialogs()
    contentInput = view.findViewById(R.id.edt_search_entries_containing_input)
    locationInput = view.findViewById(R.id.edt_search_entries_location_input)
    tagChipGroup = view.findViewById(R.id.cg_search_entries_tags)
    noTagsWarning = view.findViewById(R.id.txt_search_entries_no_tags)
    bookChipGroup = view.findViewById(R.id.cg_search_entries_books)
    noBooksWarning = view.findViewById(R.id.txt_search_entries_no_books)

    beginningDateButton = view.findViewById(R.id.btn_search_entries_beginning)
    beginningDateButton.setOnClickListener {
      startDateDialog.show(requireFragmentManager(), "Start Date Dialog")
    }

    endDateButton = view.findViewById(R.id.btn_search_entries_end)
    endDateButton.setOnClickListener {
      endDateDialog.show(requireFragmentManager(), "End Date Dialog")
    }

    searchButton = view.findViewById(R.id.btn_search_entries_go)
    searchButton.setOnClickListener {
      viewModel.search(
        contentInput.text.toString(),
        locationInput.text.toString(),
        getCheckedTags(),
        getCheckedBooks()
      )
    }

    val dragHandle: ImageView = view.findViewById(R.id.img_search_entries_drag_handle)
    dragHandle.setOnClickListener {
      if(criteriaBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
        criteriaBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
      else
        criteriaBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    resetButton = view.findViewById(R.id.btn_search_entries_reset)
    resetButton.setOnClickListener { viewModel.reset() }
  }

  private fun initDateDialogs() {
    startDateDialog = DatePickerDialog.Builder()
      .setOkClickListener(View.OnClickListener {
        viewModel.setStartDate(startDateDialog.year, startDateDialog.month, startDateDialog.day)
        startDateDialog.dismiss()
      })
      .setCancelClickListener(View.OnClickListener {
        startDateDialog.dismiss()
      })
      .showExtraButton(resources.getString(R.string.start), View.OnClickListener {
        viewModel.resetStartDate()
        startDateDialog.dismiss()
      })
      .build()

    endDateDialog = DatePickerDialog.Builder()
      .setOkClickListener(View.OnClickListener {
        viewModel.setEndDate(endDateDialog.year, endDateDialog.month, endDateDialog.day)
        endDateDialog.dismiss()
      })
      .setCancelClickListener(View.OnClickListener {
        endDateDialog.dismiss()
      })
      .showExtraButton(resources.getString(R.string.finish), View.OnClickListener {
        viewModel.resetEndDate()
        endDateDialog.dismiss()
      })
      .build()
  }

  private fun setupBottomSheetObservers() {
    viewModel.beginningDisplay.observe(this, Observer { value ->
      value?.let { beginningText ->
        if(beginningText.isBlank())
          beginningDateButton.text = resources.getString(R.string.start)
        else
          beginningDateButton.text = beginningText
      }
    })

    viewModel.endDisplay.observe(this, Observer { value ->
      value?.let { endText ->
        if(endText.isBlank())
          endDateButton.text = resources.getString(R.string.finish)
        else
          endDateButton.text = endText
      }
    })

    viewModel.tags.observe(this, Observer { value ->
      tagChipGroup.removeAllViews()

      value?.let { list ->
        list.forEach { tag ->
          val displayTag = ChipWithId(tagChipGroup.context)
          displayTag.text = tag.name
          displayTag.itemId = tag.id
          displayTag.isCheckable = true
          tagChipGroup.addView(displayTag)
        }
      }
    })

    viewModel.displayNoTagsWarning.observe(this, Observer { value ->
      value?.let { display ->
        if(display) {
          noTagsWarning.visibility = View.VISIBLE
          tagChipGroup.visibility = View.GONE
        } else {
          noTagsWarning.visibility = View.GONE
          tagChipGroup.visibility = View.VISIBLE
        }
      }
    })

    viewModel.books.observe(this, Observer { value ->
      bookChipGroup.removeAllViews()

      value?.let { list ->
        list.forEach { book ->
          val displayBook = ChipWithId(bookChipGroup.context)
          displayBook.text = book.name
          displayBook.itemId = book.id
          displayBook.isCheckable = true
          bookChipGroup.addView(displayBook)
        }
      }
    })

    viewModel.displayNoBooksWarning.observe(this, Observer { value ->
      value?.let { display ->
        if(display) {
          noBooksWarning.visibility = View.VISIBLE
          bookChipGroup.visibility = View.GONE
        } else {
          noBooksWarning.visibility = View.GONE
          bookChipGroup.visibility = View.VISIBLE
        }
      }
    })

    viewModel.dismissBottomSheet.observe(this, Observer { value ->
      value?.let { dismiss ->
        if(dismiss)
          criteriaBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
      }
    })

    viewModel.clearDisplays.observe(this, Observer { value ->
      value?.let { clear ->
        if(clear) {
          contentInput.setText("")
          locationInput.setText("")
          for(i in 0 until tagChipGroup.childCount) {
            val chip = tagChipGroup.getChildAt(i) as ChipWithId
            chip.isChecked = false
          }
          for(i in 0 until bookChipGroup.childCount) {
            val chip = bookChipGroup.getChildAt(i) as ChipWithId
            chip.isChecked = false
          }
        }
      }
    })

    viewModel.checkedTags.observe(this, Observer { value ->
      value?.let { checkedTags ->
        for(i in 0 until tagChipGroup.childCount) {
          val chip = tagChipGroup.getChildAt(i) as ChipWithId
          chip.isChecked = checkedTags.contains(chip.itemId)
        }
      }
    })

    viewModel.checkedBooks.observe(this, Observer { value ->
      value?.let { checkedBooks ->
        for(i in 0 until bookChipGroup.childCount) {
          val chip = bookChipGroup.getChildAt(i) as ChipWithId
          chip.isChecked = checkedBooks.contains(chip.itemId)
        }
      }
    })
  }

  private fun getCheckedTags(): List<Int> {
    val returnList = mutableListOf<Int>()
    for(i in 0 until tagChipGroup.childCount) {
      val chip = tagChipGroup.getChildAt(i) as ChipWithId
      if(chip.isChecked)
        returnList.add(chip.itemId)
    }
    return returnList
  }

  private fun getCheckedBooks(): List<Int> {
    val returnList = mutableListOf<Int>()
    for(i in 0 until bookChipGroup.childCount) {
      val chip = bookChipGroup.getChildAt(i) as ChipWithId
      if(chip.isChecked)
        returnList.add(chip.itemId)
    }
    return returnList
  }
}