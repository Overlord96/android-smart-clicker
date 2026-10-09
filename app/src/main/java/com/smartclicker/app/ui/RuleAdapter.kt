package com.smartclicker.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartclicker.app.databinding.ItemRuleBinding
import com.smartclicker.app.model.AutomationRule

class RuleAdapter(
    private val rules: MutableList<AutomationRule>,
    private val onRuleToggle: (AutomationRule, Boolean) -> Unit,
    private val onRuleDelete: (AutomationRule) -> Unit
) : RecyclerView.Adapter<RuleAdapter.RuleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val binding = ItemRuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        holder.bind(rules[position])
    }

    override fun getItemCount(): Int = rules.size

    fun addRule(rule: AutomationRule) {
        rules.add(rule)
        notifyItemInserted(rules.size - 1)
    }

    fun removeRule(rule: AutomationRule) {
        val index = rules.indexOf(rule)
        if (index != -1) {
            rules.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class RuleViewHolder(private val binding: ItemRuleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(rule: AutomationRule) {
            binding.tvRuleTitle.text = rule.name
            val triggers = rule.triggerTexts.joinToString(", ")
            binding.tvRuleDetails.text = "Triggers: [$triggers] | Cooldown: ${rule.clickCooldownMs}ms"

            binding.switchRuleEnabled.setOnCheckedChangeListener(null)
            binding.switchRuleEnabled.isChecked = rule.isEnabled

            binding.switchRuleEnabled.setOnCheckedChangeListener { _, isChecked ->
                rule.isEnabled = isChecked
                onRuleToggle(rule, isChecked)
            }

            binding.btnDeleteRule.setOnClickListener {
                onRuleDelete(rule)
            }
        }
    }
}
