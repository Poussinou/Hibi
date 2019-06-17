package app.marcdev.hibi.maintabs.tagsfragment.taggedentriesfragment

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import app.marcdev.hibi.internal.extension.show
import app.marcdev.hibi.maintabs.mainentriesrecycler.EntriesRecyclerAdapter
import app.marcdev.hibi.maintabs.mainentriesrecycler.MainEntriesHeaderItemDecoration
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance

class TaggedEntriesFragment : Fragment(), KodeinAware {
  override val kodein by closestKodein()

  // <editor-fold desc="View Model">
  private val viewModelFactory: TaggedEntriesViewModelFactory by instance()
  private lateinit var viewModel: TaggedEntriesViewModel
  // </editor-fold>

  // <editor-fold desc="UI Components">
  private lateinit var loadingDisplay: ConstraintLayout
  private lateinit var noResults: ConstraintLayout
  private lateinit var toolbarTitle: TextView
  private lateinit var recyclerAdapter: EntriesRecyclerAdapter
  // </editor-fold>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    viewModel = ViewModelProviders.of(this, viewModelFactory).get(TaggedEntriesViewModel::class.java)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val view = inflater.inflate(R.layout.fragment_tagged_entries, container, false)

    bindViews(view)
    initRecycler(view)
    setupObservers()
    viewModel.loadEntries()

    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    arguments?.let { arguments ->
      viewModel.passArguments(TaggedEntriesFragmentArgs.fromBundle(arguments).tagID)
    }
  }

  private fun bindViews(view: View) {
    loadingDisplay = view.findViewById(R.id.const_tagged_entries_loading)
    noResults = view.findViewById(R.id.const_no_tagged_entries)
    toolbarTitle = view.findViewById(R.id.txt_back_toolbar_title)
    val toolbarBack: ImageView = view.findViewById(R.id.img_back_toolbar_back)
    toolbarBack.setOnClickListener {
      Navigation.findNavController(requireView()).popBackStack()
    }
  }

  private fun setupObservers() {
    viewModel.toolbarTitle.observe(this, Observer { value ->
      value?.let { title ->
        toolbarTitle.text = title
      }
    })

    viewModel.entries.observe(this, Observer { value ->
      value?.let { list ->
        recyclerAdapter.updateList(list)
      }
    })

    viewModel.displayLoading.observe(this, Observer { value ->
      value?.let { shouldShow ->
        loadingDisplay.show(shouldShow)
      }
    })

    viewModel.displayNoResults.observe(this, Observer { value ->
      value?.let { shouldShow ->
        noResults.show(shouldShow)
      }
    })
  }

  private fun initRecycler(view: View) {
    val recycler: RecyclerView = view.findViewById(R.id.recycler_tagged_entries)
    this.recyclerAdapter = EntriesRecyclerAdapter(requireContext(), requireActivity().theme)
    val layoutManager = LinearLayoutManager(context)
    recycler.adapter = recyclerAdapter
    recycler.layoutManager = layoutManager

    val includeEntryDividers = PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean(PREF_ENTRY_DIVIDERS, true)
    if(includeEntryDividers) {
      val dividerItemDecoration = DividerItemDecoration(recycler.context, layoutManager.orientation)
      recycler.addItemDecoration(dividerItemDecoration)
    }

    val decoration = MainEntriesHeaderItemDecoration(recycler, recyclerAdapter)
    recycler.addItemDecoration(decoration)
  }
}