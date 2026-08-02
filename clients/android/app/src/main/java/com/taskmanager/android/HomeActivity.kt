package com.taskmanager.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.taskmanager.android.databinding.ActivityHomeBinding
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    private enum class Section { PROJECTS, ORGS }

    private lateinit var binding: ActivityHomeBinding
    private lateinit var session: SessionStore
    private lateinit var api: ApiClient
    private lateinit var adapter: EntityAdapter
    private var section = Section.PROJECTS
    private var projects = emptyList<ProjectDto>()
    private var organizations = emptyList<OrganizationDto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionStore(this)
        if (session.token == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        api = ApiClient(baseUrl = session.baseUrl, token = session.token)

        adapter = EntityAdapter { item ->
            if (section == Section.PROJECTS) {
                startActivity(
                    Intent(this, ProjectDetailActivity::class.java)
                        .putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, item.id)
                )
            } else {
                startActivity(
                    Intent(this, OrgDetailActivity::class.java)
                        .putExtra(OrgDetailActivity.EXTRA_ORG_ID, item.id)
                )
            }
        }
        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter

        binding.logoutBtn.setOnClickListener {
            session.clear()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }

        binding.sectionToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            section = if (checkedId == binding.tabOrgs.id) Section.ORGS else Section.PROJECTS
            updateFab()
            reload()
        }

        binding.fab.setOnClickListener {
            if (section == Section.PROJECTS) createProject() else createOrganization()
        }

        lifecycleScope.launch {
            runCatching {
                val me = api.me()
                binding.userLabel.text = "${me.name} · ${me.email}"
            }.onFailure {
                session.clear()
                startActivity(Intent(this@HomeActivity, AuthActivity::class.java))
                finish()
            }
        }
        updateFab()
    }

    override fun onResume() {
        super.onResume()
        if (::api.isInitialized) reload()
    }

    private fun updateFab() {
        binding.fab.text = if (section == Section.PROJECTS) {
            getString(R.string.create_project)
        } else {
            getString(R.string.create_organization)
        }
    }

    private fun reload() {
        if (section == Section.PROJECTS) loadProjects() else loadOrganizations()
    }

    private fun loadProjects() {
        lifecycleScope.launch {
            try {
                organizations = runCatching { api.listOrganizations() }.getOrDefault(emptyList())
                projects = api.listProjects()
                adapter.submit(projects.map { p ->
                    val org = p.organizationId?.let { id ->
                        organizations.find { it.id == id }?.name
                    }
                    val subtitle = p.description?.takeIf { it.isNotBlank() }
                        ?: org ?: "Личный проект"
                    EntityItem(
                        id = p.id,
                        title = p.name,
                        subtitle = subtitle,
                        badge = L10n.role(p.currentUserRole)
                    )
                })
                binding.emptyState.visibility = if (projects.isEmpty()) View.VISIBLE else View.GONE
                binding.emptyTitle.text = "Пока нет проектов"
                binding.emptyText.text = getString(R.string.empty_projects)
            } catch (e: Exception) {
                toast(e.message)
            }
        }
    }

    private fun loadOrganizations() {
        lifecycleScope.launch {
            try {
                organizations = api.listOrganizations()
                adapter.submit(organizations.map { o ->
                    EntityItem(
                        id = o.id,
                        title = o.name,
                        subtitle = "",
                        badge = L10n.role(o.currentUserRole)
                    )
                })
                binding.emptyState.visibility = if (organizations.isEmpty()) View.VISIBLE else View.GONE
                binding.emptyTitle.text = "Создайте организацию"
                binding.emptyText.text = getString(R.string.how_orgs_work)
            } catch (e: Exception) {
                toast(e.message)
            }
        }
    }

    private fun createOrganization() {
        askText("Новая организация", "Название") { name ->
            lifecycleScope.launch {
                try {
                    val org = api.createOrganization(name, "LOCAL")
                    toast("Организация создана")
                    startActivity(
                        Intent(this@HomeActivity, OrgDetailActivity::class.java)
                            .putExtra(OrgDetailActivity.EXTRA_ORG_ID, org.id)
                    )
                } catch (e: Exception) {
                    toast(e.message)
                }
            }
        }
    }

    private fun createProject() {
        lifecycleScope.launch {
            val orgs = runCatching { api.listOrganizations() }.getOrDefault(emptyList())
            val labels = mutableListOf("Личный проект")
            labels += orgs.map { "В организации: ${it.name}" }
            askChoice("Где создать проект?", labels) { which ->
                val orgId = if (which == 0) null else orgs[which - 1].id
                askText("Новый проект", "Название проекта") { name ->
                    lifecycleScope.launch {
                        try {
                            val project = api.createProject(name, null, false, true, orgId)
                            toast("Проект создан")
                            startActivity(
                                Intent(this@HomeActivity, ProjectDetailActivity::class.java)
                                    .putExtra(ProjectDetailActivity.EXTRA_PROJECT_ID, project.id)
                            )
                        } catch (e: Exception) {
                            toast(e.message)
                        }
                    }
                }
            }
        }
    }
}
