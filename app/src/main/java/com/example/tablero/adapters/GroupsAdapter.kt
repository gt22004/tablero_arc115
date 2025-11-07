package com.example.espdisplay.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.espdisplay.R
import com.example.espdisplay.models.Group
import com.google.android.material.card.MaterialCardView

class GroupsAdapter(
    private val onGroupClick: (Group) -> Unit,
    private val onGroupLongClick: (Group) -> Unit
) : ListAdapter<Group, GroupsAdapter.GroupViewHolder>(GroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = getItem(position)
        holder.bind(group)
    }

    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.groupCard)
        private val nameText: TextView = itemView.findViewById(R.id.groupName)
        private val numberText: TextView = itemView.findViewById(R.id.groupNumber)

        fun bind(group: Group) {
            nameText.text = group.name
            numberText.text = "#${group.groupNumber}"

            card.setOnClickListener { onGroupClick(group) }
            card.setOnLongClickListener {
                onGroupLongClick(group)
                true
            }
        }
    }

    class GroupDiffCallback : DiffUtil.ItemCallback<Group>() {
        override fun areItemsTheSame(oldItem: Group, newItem: Group): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Group, newItem: Group): Boolean {
            return oldItem == newItem
        }
    }

}

