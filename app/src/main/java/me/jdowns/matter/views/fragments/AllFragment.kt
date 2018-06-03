package me.jdowns.matter.views.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import me.jdowns.matter.Matter
import me.jdowns.matter.R
import me.jdowns.matter.views.adapters.SubmissionAdapter
import net.dean.jraw.models.Submission
import net.dean.jraw.pagination.DefaultPaginator

class AllFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, SubmissionAdapter.SubmissionRecyclerViewListener {
    private var paginator: DefaultPaginator<Submission>? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val dataSet: MutableList<Submission> = mutableListOf()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_all, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_all).apply {
            adapter = SubmissionAdapter(dataSet).apply {
                listener = this@AllFragment
            }
            layoutManager = LinearLayoutManager(context).apply {
                orientation = LinearLayout.VERTICAL
            }
        }
        swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_layout_view_all).apply {
            setOnRefreshListener(this@AllFragment)
            post {
                isRefreshing = true
                onRefresh()
            }
        }

        getNextPage()
    }

    override fun onRefresh() {
        if (dataSet.size > 0) {
            /** TODO: Resolve why calling clear on the data set caused an index out of bounds */
            val tempDataSet = mutableListOf<Submission>()
            tempDataSet.addAll(dataSet)
            for (submission in tempDataSet) {
                dataSet.remove(submission)
            }
            recyclerView.adapter.notifyDataSetChanged()
        }
        paginator = null
        getNextPage()
    }

    override fun atBottom() {
        getNextPage()
    }

    private fun getNextPage() {
        async {
            if (paginator == null) {
                /** TODO: Implement full OAuth */
                // This is used for general testing
                paginator = Matter.accountHelper.switchToUserless().subreddit("all").posts().limit(25).build()
            }
            val newDataSet = paginator!!.next()
            dataSet.addAll(newDataSet)
            launch(UI) {
                recyclerView.adapter.notifyItemRangeInserted(recyclerView.adapter.itemCount, newDataSet.size - 1)
                stopLoading()
            }
        }

    }

    private fun stopLoading() {
        swipeRefreshLayout.isRefreshing = false
        with(recyclerView) {
            animate()
                .withStartAction { visibility = View.VISIBLE }
                .alpha(1.0F)
        }
    }

    companion object {
        const val FRAGMENT_TAG = "allFragmentTag"
    }
}