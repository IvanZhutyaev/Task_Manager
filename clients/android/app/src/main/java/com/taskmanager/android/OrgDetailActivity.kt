package com.taskmanager.android

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.taskmanager.android.databinding.ActivityOrgDetailBinding
import kotlinx.coroutines.launch

class OrgDetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ORG_ID = "org_id"
    }

    private lateinit var binding: ActivityOrgDetailBinding
    private lateinit var api: ApiClient
    private var orgId: Long = -1
    private lateinit var membersAdapter: PersonAdapter
    private lateinit var teamsAdapter: TeamAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orgId = intent.getLongExtra(EXTRA_ORG_ID, -1)
        if (orgId < 0) {
            finish()
            return
        }

        binding = ActivityOrgDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val session = SessionStore(this)
        api = ApiClient(baseUrl = session.baseUrl, token = session.token)

        membersAdapter = PersonAdapter()
        teamsAdapter = TeamAdapter { team -> addToTeam(team) }
        binding.membersList.layoutManager = LinearLayoutManager(this)
        binding.membersList.adapter = membersAdapter
        binding.teamsList.layoutManager = LinearLayoutManager(this)
        binding.teamsList.adapter = teamsAdapter

        binding.backBtn.setOnClickListener { finish() }
        binding.addMemberBtn.setOnClickListener { addOrgMember() }
        binding.createTeamBtn.setOnClickListener { createTeam() }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        lifecycleScope.launch {
            try {
                val orgs = api.listOrganizations()
                val org = orgs.find { it.id == orgId } ?: api.listOrganizations().find { it.id == orgId }
                binding.orgTitle.text = org?.name ?: "Организация"
                val roleLabel = L10n.role(org?.currentUserRole)
                binding.orgMeta.text = if (roleLabel.isNotBlank()) "Ваша роль: $roleLabel" else ""
                binding.orgMeta.visibility = if (roleLabel.isNotBlank()) View.VISIBLE else View.GONE

                val members = api.listOrgMembers(orgId)
                membersAdapter.submit(members.map {
                    Triple(it.name ?: it.email, it.email, it.role)
                })
                binding.membersEmpty.visibility = if (members.isEmpty()) View.VISIBLE else View.GONE

                val teams = api.listTeams(orgId)
                val teamRows = teams.map { team ->
                    val people = runCatching { api.listTeamMembers(orgId, team.id) }.getOrDefault(emptyList())
                    val text = if (people.isEmpty()) "Пока никого в команде"
                    else people.joinToString("\n") { "• ${it.name ?: it.email}" }
                    team to text
                }
                teamsAdapter.submit(teamRows)
                binding.teamsEmpty.visibility = if (teams.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                toast(e.message)
            }
        }
    }

    private fun addOrgMember() {
        var roleIndex = 0
        val input = android.widget.EditText(this).apply {
            hint = "email@example.com"
            setPadding(48, 36, 48, 36)
        }
        AlertDialog.Builder(this)
            .setTitle("Добавить участника организации")
            .setMessage("Человек должен уже иметь аккаунт Task Manager.")
            .setView(input)
            .setSingleChoiceItems(L10n.orgMemberRoleLabels.toTypedArray(), 0) { _, which ->
                roleIndex = which
            }
            .setPositiveButton("Добавить") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isBlank()) return@setPositiveButton
                val role = L10n.orgMemberRoleValues[roleIndex]
                lifecycleScope.launch {
                    try {
                        api.addOrgMember(orgId, email, role)
                        toast("Участник добавлен")
                        reload()
                    } catch (e: Exception) {
                        toast(e.message)
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun createTeam() {
        askText("Новая команда", "Например: Backend") { name ->
            lifecycleScope.launch {
                try {
                    api.createTeam(orgId, name)
                    toast("Команда создана")
                    reload()
                } catch (e: Exception) {
                    toast(e.message)
                }
            }
        }
    }

    private fun addToTeam(team: TeamDto) {
        askText(
            title = "В команду «${team.name}»",
            hint = "email участника организации",
            message = "Сначала добавьте человека в организацию, затем сюда."
        ) { email ->
            lifecycleScope.launch {
                try {
                    api.addTeamMember(orgId, team.id, email)
                    toast("Добавлен в команду")
                    reload()
                } catch (e: Exception) {
                    toast(e.message)
                }
            }
        }
    }
}
