package com.taskmanager.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.taskmanager.android.databinding.ActivityBoardBinding
import com.taskmanager.android.databinding.ItemColumnBinding
import kotlinx.coroutines.launch

class BoardActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PROJECT_ID = "project_id"
        const val EXTRA_BOARD_ID = "board_id"
        const val EXTRA_BOARD_NAME = "board_name"
        const val EXTRA_ACCESS_MODE = "access_mode"
    }

    private lateinit var binding: ActivityBoardBinding
    private lateinit var api: ApiClient
    private var projectId: Long = -1
    private var boardId: Long = -1
    private var columns: List<ColumnDto> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectId = intent.getLongExtra(EXTRA_PROJECT_ID, -1)
        boardId = intent.getLongExtra(EXTRA_BOARD_ID, -1)
        if (boardId < 0) {
            finish()
            return
        }

        binding = ActivityBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val session = SessionStore(this)
        api = ApiClient(baseUrl = session.baseUrl, token = session.token)

        binding.boardTitle.text = intent.getStringExtra(EXTRA_BOARD_NAME) ?: "Доска"
        binding.boardMeta.text = "Доступ: ${intent.getStringExtra(EXTRA_ACCESS_MODE) ?: "OPEN"}"

        binding.backBtn.setOnClickListener { finish() }
        binding.newColumnBtn.setOnClickListener { createColumn() }
        binding.newTaskBtn.setOnClickListener { createTask() }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        lifecycleScope.launch {
            try {
                columns = api.listColumns(boardId)
                binding.columnsContainer.removeAllViews()
                binding.boardEmpty.visibility = if (columns.isEmpty()) View.VISIBLE else View.GONE

                val inflater = LayoutInflater.from(this@BoardActivity)
                for (column in columns) {
                    val colBinding = ItemColumnBinding.inflate(inflater, binding.columnsContainer, false)
                    colBinding.columnTitle.text = column.name
                    val taskAdapter = TaskAdapter()
                    colBinding.tasksList.layoutManager = LinearLayoutManager(this@BoardActivity)
                    colBinding.tasksList.adapter = taskAdapter
                    val tasks = api.listTasks(column.id)
                    taskAdapter.submit(tasks)
                    binding.columnsContainer.addView(colBinding.root)
                }
            } catch (e: Exception) {
                toast(e.message)
            }
        }
    }

    private fun createColumn() {
        askText("Новая колонка", "Например: Todo") { name ->
            lifecycleScope.launch {
                try {
                    api.createColumn(boardId, name)
                    toast("Колонка создана")
                    reload()
                } catch (e: Exception) {
                    toast(e.message)
                }
            }
        }
    }

    private fun createTask() {
        if (columns.isEmpty()) {
            toast("Сначала создайте колонку")
            return
        }
        askChoice("В какую колонку?", columns.map { it.name }) { index ->
            val column = columns[index]
            askText("Новая задача", "Заголовок") { title ->
                lifecycleScope.launch {
                    try {
                        api.createTask(column.id, title)
                        toast("Задача создана")
                        reload()
                    } catch (e: Exception) {
                        toast(e.message)
                    }
                }
            }
        }
    }
}
