package org.gault.messages.adapters

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Parcelable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller
import org.gault.commons.adapters.MyRecyclerViewListAdapter
import org.gault.commons.extensions.*
import org.gault.commons.helpers.FontHelper
import org.gault.commons.helpers.SimpleContactsHelper
import org.gault.commons.helpers.ensureBackgroundThread
import org.gault.commons.views.MyRecyclerView
import org.gault.messages.activities.SimpleActivity
import org.gault.messages.databinding.ItemConversationBinding
import org.gault.messages.extensions.config
import org.gault.messages.extensions.getAllDrafts
import org.gault.messages.models.Conversation

@Suppress("LeakingThis")
abstract class BaseConversationsAdapter(
    activity: SimpleActivity,
    recyclerView: MyRecyclerView,
    onRefresh: () -> Unit,
    itemClick: (Any) -> Unit,
) : MyRecyclerViewListAdapter<Conversation>(
    activity = activity,
    recyclerView = recyclerView,
    diffUtil = ConversationDiffCallback(),
    itemClick = itemClick,
    onRefresh = onRefresh
), RecyclerViewFastScroller.OnPopupTextUpdate {

    private var fontSize = activity.getTextSize()
    private var drafts = HashMap<Long, String>()
    private var recyclerViewState: Parcelable? = null
    
    // --- GAULT FILTER STATE ---
    private var allConversations = ArrayList<Conversation>()
    private var showOnlyP2P = false

    init {
        setupDragListener(true)
        setHasStableIds(true)
        updateDrafts()

        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = restoreRecyclerViewState()
            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = restoreRecyclerViewState()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = restoreRecyclerViewState()
        })
    }

    // --- GAULT FILTER LOGIC ---
    fun toggleGaultFilter(enable: Boolean) {
        showOnlyP2P = enable
        applyFilter()
    }

    private fun applyFilter(commitCallback: (() -> Unit)? = null) {
        val filtered = if (showOnlyP2P) {
            allConversations.filter { it.isP2P() }
        } else {
            allConversations
        }
        submitList(filtered.toList(), commitCallback)
    }

    fun updateConversations(newConversations: ArrayList<Conversation>, commitCallback: (() -> Unit)? = null) {
        allConversations = newConversations
        saveRecyclerViewState()
        applyFilter(commitCallback)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateFontSize() {
        fontSize = activity.getTextSize()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateDrafts() {
        ensureBackgroundThread {
            val newDrafts = HashMap<Long, String>()
            fetchDrafts(newDrafts)
            activity.runOnUiThread {
                if (drafts.hashCode() != newDrafts.hashCode()) {
                    drafts = newDrafts
                    notifyDataSetChanged()
                }
            }
        }
    }

    override fun getSelectableItemCount() = itemCount
    protected fun getSelectedItems() = currentList.filter { selectedKeys.contains(it.hashCode()) } as ArrayList<Conversation>
    override fun getIsItemSelectable(position: Int) = true
    override fun getItemSelectionKey(position: Int) = currentList.getOrNull(position)?.hashCode()
    override fun getItemKeyPosition(key: Int) = currentList.indexOfFirst { it.hashCode() == key }
    override fun onActionModeCreated() {}
    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConversationBinding.inflate(layoutInflater, parent, false)
        return createViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = getItem(position)
        holder.bindView(conversation, true, true) { itemView, _ ->
            setupView(itemView, conversation)
        }
        bindViewHolder(holder)
    }

    override fun getItemId(position: Int) = getItem(position).threadId

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed && !activity.isFinishing) {
            val itemView = ItemConversationBinding.bind(holder.itemView)
            Glide.with(activity).clear(itemView.conversationImage)
        }
    }

    private fun fetchDrafts(drafts: HashMap<Long, String>) {
        drafts.clear()
        for ((threadId, draft) in activity.getAllDrafts()) {
            drafts[threadId] = draft
        }
    }

    private fun setupView(view: View, conversation: Conversation) {
        ItemConversationBinding.bind(view).apply {
            root.setupViewBackground(activity)
            val smsDraft = drafts[conversation.threadId]
            
            draftIndicator.beVisibleIf(!smsDraft.isNullOrEmpty())
            draftIndicator.setTextColor(properPrimaryColor)

            pinIndicator.beVisibleIf(activity.config.pinnedConversations.contains(conversation.threadId.toString()))
            pinIndicator.applyColorFilter(textColor)

            conversationFrame.isSelected = selectedKeys.contains(conversation.hashCode())

            conversationAddress.apply {
                text = conversation.title
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 1.2f)
            }

            // --- GAULT BODY OBSERVATION ---
            conversationBodyShort.apply {
                val baseText = smsDraft ?: conversation.snippet
                text = if (conversation.isP2P()) "[Secure] $baseText" else baseText
                
                // Colorize P2P threads to show field intensity
                if (conversation.isP2P()) {
                    setTextColor(properPrimaryColor)
                } else {
                    setTextColor(textColor)
                }
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.9f)
            }

            conversationDate.apply {
                text = (conversation.date * 1000L).formatDateOrTime(context, true, false)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.8f)
            }

            val isUnread = !conversation.read
            val style = if (isUnread) {
                conversationBodyShort.alpha = 1f
                if (conversation.isScheduled) Typeface.BOLD_ITALIC else Typeface.BOLD
            } else {
                conversationBodyShort.alpha = 0.7f
                if (conversation.isScheduled) Typeface.ITALIC else Typeface.NORMAL
            }
            
            val customTypeface = FontHelper.getTypeface(activity)
            conversationAddress.setTypeface(customTypeface, style)
            conversationBodyShort.setTypeface(customTypeface, style)
            conversationDate.setTypeface(customTypeface, style)

            // Override text colors for non-P2P fields
            if (!conversation.isP2P()) {
                arrayListOf(conversationAddress, conversationDate).forEach { it.setTextColor(textColor) }
            } else {
                conversationDate.setTextColor(textColor)
                conversationAddress.setTextColor(textColor)
            }

            setupBadgeCount(unreadCountBadge, isUnread, conversation.unreadCount)
            
            val placeholder = if (conversation.isGroupConversation) {
                SimpleContactsHelper(activity).getColoredGroupIcon(conversation.title)
            } else null

            SimpleContactsHelper(activity).loadContactImage(
                path = conversation.photoUri,
                imageView = conversationImage,
                placeholderName = conversation.title,
                placeholderImage = placeholder
            )
        }
    }

    private fun setupBadgeCount(view: TextView, isUnread: Boolean, count: Int) {
        view.apply {
            beVisibleIf(isUnread)
            if (isUnread) {
                text = when {
                    count > MAX_UNREAD_BADGE_COUNT -> "$MAX_UNREAD_BADGE_COUNT+"
                    count == 0 -> ""
                    else -> count.toString()
                }
                setTextColor(properPrimaryColor.getContrastColor())
                background?.applyColorFilter(properPrimaryColor)
            }
        }
    }

    override fun onChange(position: Int) = currentList.getOrNull(position)?.title ?: ""

    private fun saveRecyclerViewState() {
        recyclerViewState = recyclerView.layoutManager?.onSaveInstanceState()
    }

    private fun restoreRecyclerViewState() {
        recyclerView.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    private class ConversationDiffCallback : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation) = Conversation.areItemsTheSame(oldItem, newItem)
        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation) = Conversation.areContentsTheSame(oldItem, newItem)
    }

    companion object {
        private const val MAX_UNREAD_BADGE_COUNT = 99
    }
}