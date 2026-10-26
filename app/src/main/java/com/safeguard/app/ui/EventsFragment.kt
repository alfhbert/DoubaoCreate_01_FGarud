package com.safeguard.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.safeguard.app.R
import com.safeguard.app.util.EventsStore

class EventsFragment : Fragment() {

    private var adapter: EventAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_events, container, false)
        val rv = v.findViewById<RecyclerView>(R.id.rv_events)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = EventAdapter()
        rv.adapter = adapter
        v.findViewById<View>(R.id.btn_clear_events).setOnClickListener {
            EventsStore.clear(requireContext())
            adapter?.update(EventsStore.load(requireContext()))
        }
        return v
    }

    override fun onResume() {
        super.onResume()
        adapter?.update(EventsStore.load(requireContext()))
    }
}

class EventAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<EventAdapter.VH>() {

    private val items = mutableListOf<EventsStore.Event>()

    fun update(list: List<EventsStore.Event>) {
        items.clear()
        items.addAll(list.sortedByDescending { it.ts })
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = items[position]
        holder.time.text = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA)
            .format(java.util.Date(e.ts))
        holder.title.text = e.title
        holder.detail.text = e.detail
    }

    override fun getItemCount(): Int = items.size

    class VH(v: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(v) {
        val time: android.widget.TextView = v.findViewById(R.id.tv_event_time)
        val title: android.widget.TextView = v.findViewById(R.id.tv_event_title)
        val detail: android.widget.TextView = v.findViewById(R.id.tv_event_detail)
    }
}
