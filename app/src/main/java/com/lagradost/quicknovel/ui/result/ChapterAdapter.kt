package com.lagradost.quicknovel.ui.result

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import com.lagradost.quicknovel.ChapterData
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.databinding.SimpleChapterBinding
import com.lagradost.quicknovel.ui.BaseDiffCallback
import com.lagradost.quicknovel.ui.NoStateAdapter
import com.lagradost.quicknovel.ui.ViewHolderState
import com.lagradost.quicknovel.ui.newSharedPool
import com.lagradost.quicknovel.util.UIHelper.popupMenu

class ChapterAdapter(val viewModel: ResultViewModel) :
    NoStateAdapter<ChapterData>(
        diffCallback = BaseDiffCallback(
            itemSame = { a, b -> a.url == b.url },
            contentSame = { a, b -> a == b }
        )) {

    companion object {
        val sharedPool =
            newSharedPool {
                setMaxRecycledViews(CONTENT, 10)
            }
    }

    override fun onCreateContent(parent: ViewGroup): ViewHolderState<Any> {
        return ViewHolderState(
            SimpleChapterBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    private fun refresh(
        binding: SimpleChapterBinding,
        card: ChapterData,
        viewModel: ResultViewModel
    ) {
        val alpha = if (viewModel.hasReadChapter(chapter = card)) 0.5F else 1.0F

        binding.name.alpha = alpha
        binding.releaseDate.alpha = alpha
    }

    override fun onBindContent(holder: ViewHolderState<Any>, item: ChapterData, position: Int) {
        val binding = holder.view as? SimpleChapterBinding ?: return
        binding.apply {
            name.text = item.name
            releaseDate.text = item.dateOfRelease
            releaseDate.isGone = item.dateOfRelease.isNullOrBlank()
            root.setOnClickListener {
                viewModel.streamRead(item)
                viewModel.isResume = true//to update read status
            }
            root.setOnLongClickListener { view ->
                val hasRead = viewModel.hasReadChapter(item)
                val actions = if (hasRead) {
                    listOf(R.string.mark_as_unread, R.string.mark_as_unread_up_to_this_chapter)
                } else {
                    listOf(R.string.mark_as_read, R.string.mark_as_read_up_to_this_chapter)
                }
                view.popupMenu(actions.map { it to it }, null) {
                    when (itemId) {
                        R.string.mark_as_read, R.string.mark_as_unread -> {
                            viewModel.setReadChapter(chapter = item, !hasRead)
                            refresh(binding, item, viewModel)
                        }

                        R.string.mark_as_read_up_to_this_chapter, R.string.mark_as_unread_up_to_this_chapter -> {
                            viewModel.setReadChaptersUpTo(chapter = item, !hasRead)
                            // the previous chapters changed too, so refresh every visible chapter
                            notifyItemRangeChanged(0, itemCount)
                        }
                    }
                }
                return@setOnLongClickListener true
            }
            refresh(binding, item, viewModel)
        }
    }
}