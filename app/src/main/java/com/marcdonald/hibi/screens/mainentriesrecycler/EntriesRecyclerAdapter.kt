/*
 * Copyright 2019 Marc Donald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marcdonald.hibi.screens.mainentriesrecycler

import android.content.Context
import android.content.res.Resources
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.marcdonald.hibi.R
import com.marcdonald.hibi.internal.PREF_RECYCLER_ANIMATIONS

class EntriesRecyclerAdapter(private val context: Context,
														 private val onClick: (Int) -> Unit,
														 private val theme: Resources.Theme)
	: RecyclerView.Adapter<BaseEntriesRecyclerViewHolder>() {

	private val inflater: LayoutInflater = LayoutInflater.from(context)
	private var items: List<MainEntriesDisplayItem> = listOf()
	private var lastPosition = -1
	private var itemsSelectable = false
	private var onSelectClick: View.OnClickListener? = null
	private var animateEntries = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_RECYCLER_ANIMATIONS, true)

	private var _hasSelectedItems = MutableLiveData<Boolean>()
	val hasSelectedItems: LiveData<Boolean>
		get() = _hasSelectedItems

	constructor(context: Context, onClick: (Int) -> Unit, itemsSelectable: Boolean, onSelectClick: View.OnClickListener, theme: Resources.Theme) : this(context, onClick, theme) {
		this.itemsSelectable = itemsSelectable
		this.onSelectClick = onSelectClick
	}

	override fun getItemViewType(position: Int): Int {
		// Use day as the view type, if the day is 0 then it's a header, since a day 0 can't be added normally
		return items[position].entry.day
	}

	fun isHeader(itemPosition: Int): Boolean {
		return items[itemPosition].entry.day == 0
	}

	fun getSelectedEntryIds(): List<Int> {
		val list = mutableListOf<Int>()
		items.forEach { item ->
			if(item.isSelected)
				list.add(item.entry.id)
		}
		return list.toList()
	}

	fun clearSelectedEntries() {
		items.forEach { item ->
			item.isSelected = false
		}
		_hasSelectedItems.value = false
		notifyDataSetChanged()
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseEntriesRecyclerViewHolder {
		return when(viewType) {
			0    -> {
				val view = inflater.inflate(R.layout.item_main_screen_header, parent, false)
				EntriesRecyclerViewHolderHeader(view)
			}
			else -> {
				val view = inflater.inflate(R.layout.item_main_screen_entry, parent, false)
				EntriesRecyclerViewHolder(onClick, onSelectClick, view, theme)
			}
		}
	}

	override fun getItemCount(): Int {
		return items.size
	}

	override fun onBindViewHolder(holder: BaseEntriesRecyclerViewHolder, position: Int) {
		holder.display(items[position])
		if(itemsSelectable) {
			if(!isHeader(position)) {
				holder.itemView.findViewById<ConstraintLayout>(R.id.const_item_main_recycler).setOnLongClickListener {
					items[position].isSelected = !items[position].isSelected
					_hasSelectedItems.value = getSelectedEntryIds().isNotEmpty()
					notifyItemChanged(position)
					true
				}
			}
		}

		if(animateEntries)
			setAnimation(holder.itemView, position)
	}

	fun updateList(list: List<MainEntriesDisplayItem>) {
		items = list
		notifyDataSetChanged()
	}

	private fun setAnimation(viewToAnimate: View, position: Int) {
		if(position > lastPosition) {
			val animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
			viewToAnimate.startAnimation(animation)
			lastPosition = position
		}
	}
}