package com.safeguard.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.safeguard.app.R
import com.safeguard.app.databinding.FragmentEventsBinding
import com.safeguard.app.util.EventsStore
import com.safeguard.app.util.LocationUtils

class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: EventAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = EventAdapter()
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        binding.btnClearEvents.setOnClickListener {
            EventsStore.clear(requireContext())
            reload()
        }
        reload()
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun reload() {
        val list = EventsStore.load(requireContext())
        adapter.submit(list)
        binding.textEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private class EventAdapter : RecyclerView.Adapter<EventAdapter.VH>() {

        private val items = mutableListOf<EventsStore.Ev>()

        fun submit(list: List<EventsStore.Ev>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_event, parent, false)
            return VH(v)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val ev = items[position]
            holder.title.text = ev.title
            holder.time.text = LocationUtils.formatTime(ev.ts)
            holder.detail.text = ev.detail
        }

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.textEventTitle)
            val time: TextView = v.findViewById(R.id.textEventTime)
            val detail: TextView = v.findViewById(R.id.textEventDetail)
        }
    }
}
