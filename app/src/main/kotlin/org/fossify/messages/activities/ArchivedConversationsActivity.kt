package org.gault.messages.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import org.gault.commons.dialogs.ConfirmationDialog
import org.gault.commons.extensions.areSystemAnimationsEnabled
import org.gault.commons.extensions.beGoneIf
import org.gault.commons.extensions.beVisibleIf
import org.gault.commons.extensions.getProperBackgroundColor
import org.gault.commons.extensions.getProperTextColor
import org.gault.commons.extensions.hideKeyboard
import org.gault.commons.extensions.viewBinding
import org.gault.commons.helpers.NavigationIcon
import org.gault.commons.helpers.ensureBackgroundThread
import org.gault.messages.R
import org.gault.messages.adapters.ArchivedConversationsAdapter
import org.gault.messages.databinding.ActivityArchivedConversationsBinding
import org.gault.messages.extensions.config
import org.gault.messages.extensions.conversationsDB
import org.gault.messages.extensions.removeAllArchivedConversations
import org.gault.messages.helpers.THREAD_ID
import org.gault.messages.helpers.THREAD_TITLE
import org.gault.messages.models.Conversation
import org.gault.messages.models.Events
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ArchivedConversationsActivity : SimpleActivity() {
    private var bus: EventBus? = null
    private val binding by viewBinding(ActivityArchivedConversationsBinding::inflate)

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupOptionsMenu()

        setupEdgeToEdge(padBottomImeAndSystem = listOf(binding.conversationsList))
        setupMaterialScrollListener(
            scrollingView = binding.conversationsList,
            topAppBar = binding.archiveAppbar
        )

        loadArchivedConversations()
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.archiveAppbar, NavigationIcon.Arrow)
        loadArchivedConversations()
    }

    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)
    }

    private fun setupOptionsMenu() {
        binding.archiveToolbar.inflateMenu(R.menu.archive_menu)
        binding.archiveToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.empty_archive -> removeAll()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun updateOptionsMenu(conversations: ArrayList<Conversation>) {
        binding.archiveToolbar.menu.apply {
            findItem(R.id.empty_archive).isVisible = conversations.isNotEmpty()
        }
    }

    private fun loadArchivedConversations() {
        ensureBackgroundThread {
            val conversations = try {
                conversationsDB.getAllArchived().toMutableList() as ArrayList<Conversation>
            } catch (e: Exception) {
                ArrayList()
            }

            runOnUiThread {
                setupConversations(conversations)
            }
        }

        bus = EventBus.getDefault()
        try {
            bus!!.register(this)
        } catch (ignored: Exception) {
        }
    }

    private fun removeAll() {
        ConfirmationDialog(
            activity = this,
            message = "",
            messageId = R.string.empty_archive_confirmation,
            positive = org.gault.commons.R.string.yes,
            negative = org.gault.commons.R.string.no
        ) {
            removeAllArchivedConversations {
                loadArchivedConversations()
            }
        }
    }

    private fun getOrCreateConversationsAdapter(): ArchivedConversationsAdapter {
        var currAdapter = binding.conversationsList.adapter
        if (currAdapter == null) {
            hideKeyboard()
            currAdapter = ArchivedConversationsAdapter(
                activity = this,
                recyclerView = binding.conversationsList,
                onRefresh = { notifyDatasetChanged() },
                itemClick = { handleConversationClick(it) }
            )

            binding.conversationsList.adapter = currAdapter
            if (areSystemAnimationsEnabled) {
                binding.conversationsList.scheduleLayoutAnimation()
            }
        }
        return currAdapter as ArchivedConversationsAdapter
    }

    private fun setupConversations(conversations: ArrayList<Conversation>) {
        val sortedConversations = conversations.sortedWith(
            compareByDescending<Conversation> { config.pinnedConversations.contains(it.threadId.toString()) }
                .thenByDescending { it.date }
        ).toMutableList() as ArrayList<Conversation>

        showOrHidePlaceholder(conversations.isEmpty())
        updateOptionsMenu(conversations)

        try {
            getOrCreateConversationsAdapter().apply {
                updateConversations(sortedConversations)
            }
        } catch (ignored: Exception) {
        }
    }

    private fun showOrHidePlaceholder(show: Boolean) {
        binding.conversationsFastscroller.beGoneIf(show)
        binding.noConversationsPlaceholder.beVisibleIf(show)
        binding.noConversationsPlaceholder.setTextColor(getProperTextColor())
        binding.noConversationsPlaceholder.text = getString(R.string.no_archived_conversations)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun notifyDatasetChanged() {
        getOrCreateConversationsAdapter().notifyDataSetChanged()
    }

    private fun handleConversationClick(any: Any) {
        Intent(this, ThreadActivity::class.java).apply {
            val conversation = any as Conversation
            putExtra(THREAD_ID, conversation.threadId)
            putExtra(THREAD_TITLE, conversation.title)
            startActivity(this)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshConversations(@Suppress("unused") event: Events.RefreshConversations) {
        loadArchivedConversations()
    }
}
