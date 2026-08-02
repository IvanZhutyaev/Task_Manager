package com.taskmanager.android

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.taskmanager.android.databinding.ItemBoardBinding
import com.taskmanager.android.databinding.ItemEntityBinding
import com.taskmanager.android.databinding.ItemPersonBinding
import com.taskmanager.android.databinding.ItemTaskBinding
import com.taskmanager.android.databinding.ItemTeamBinding

fun Context.toast(message: String?) =
    Toast.makeText(this, message ?: "Ошибка", Toast.LENGTH_LONG).show()

fun Context.askText(
    title: String,
    hint: String,
    message: String? = null,
    onOk: (String) -> Unit
) {
    val input = EditText(this).apply {
        this.hint = hint
        setPadding(48, 36, 48, 36)
    }
    val builder = AlertDialog.Builder(this).setTitle(title).setView(input)
    if (message != null) builder.setMessage(message)
    builder.setPositiveButton("OK") { _, _ ->
        val value = input.text.toString().trim()
        if (value.isNotBlank()) onOk(value)
    }.setNegativeButton("Отмена", null).show()
}

fun Context.askChoice(title: String, labels: List<String>, onPick: (Int) -> Unit) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setItems(labels.toTypedArray()) { _, which -> onPick(which) }
        .setNegativeButton("Отмена", null)
        .show()
}

data class EntityItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val badge: String
)

class EntityAdapter(
    private val onClick: (EntityItem) -> Unit
) : RecyclerView.Adapter<EntityAdapter.VH>() {
    private val items = mutableListOf<EntityItem>()

    fun submit(data: List<EntityItem>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemEntityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(private val binding: ItemEntityBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: EntityItem) {
            binding.title.text = item.title
            binding.subtitle.text = item.subtitle
            binding.badge.text = item.badge
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}

class PersonAdapter : RecyclerView.Adapter<PersonAdapter.VH>() {
    private val items = mutableListOf<Triple<String, String, String>>()

    fun submit(data: List<Triple<String, String, String>>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPersonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    class VH(private val binding: ItemPersonBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Triple<String, String, String>) {
            binding.name.text = item.first
            binding.email.text = item.second
            binding.role.text = item.third
            binding.role.visibility = if (item.third.isBlank()) View.GONE else View.VISIBLE
        }
    }
}

class TeamAdapter(
    private val onAddMember: (TeamDto) -> Unit
) : RecyclerView.Adapter<TeamAdapter.VH>() {
    private val items = mutableListOf<Pair<TeamDto, String>>()

    fun submit(data: List<Pair<TeamDto, String>>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTeamBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(private val binding: ItemTeamBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Pair<TeamDto, String>) {
            binding.teamName.text = item.first.name
            binding.teamMembers.text = item.second
            binding.addMemberBtn.setOnClickListener { onAddMember(item.first) }
        }
    }
}

class BoardAdapter(
    private val onClick: (BoardDto) -> Unit
) : RecyclerView.Adapter<BoardAdapter.VH>() {
    private val items = mutableListOf<BoardDto>()

    fun submit(data: List<BoardDto>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemBoardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(private val binding: ItemBoardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: BoardDto) {
            binding.boardName.text = item.name
            binding.boardMeta.text = "Доступ: ${item.accessMode ?: "OPEN"}"
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}

class TaskAdapter : RecyclerView.Adapter<TaskAdapter.VH>() {
    private val items = mutableListOf<TaskDto>()

    fun submit(data: List<TaskDto>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    class VH(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TaskDto) {
            binding.taskTitle.text = item.title
            val who = item.assigneeName ?: "без исполнителя"
            binding.taskMeta.text = "${item.priority} · ${item.status} · $who"
        }
    }
}
