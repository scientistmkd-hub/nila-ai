package com.example.core.memory

import com.example.core.model.MemoryCategory
import java.util.Locale

sealed class MemoryIntentResult {
    data class SaveMemoryPrompt(
        val title: String,
        val content: String,
        val category: MemoryCategory,
        val personName: String? = null,
        val relationship: String? = null
    ) : MemoryIntentResult()

    data class LookupQuery(
        val query: String,
        val targetType: String // "PERSON", "OBJECT", "GENERAL"
    ) : MemoryIntentResult()

    data class DeletePrompt(val query: String) : MemoryIntentResult()
    object ClearAllPrompt : MemoryIntentResult()
    object None : MemoryIntentResult()
}

object MemoryIntentParser {

    fun parse(input: String): MemoryIntentResult {
        val clean = input.trim().lowercase(Locale.ROOT)
            .replace(Regex("^(nila|hey nila|hello nila)[, ]*"), "")
            .trim()

        // 1. Clear All Memory
        if (clean == "clear my memory" || clean == "delete all memories" || clean == "wipe memory" || clean == "clear all memory") {
            return MemoryIntentResult.ClearAllPrompt
        }

        // 2. Forget / Delete specific memory
        if (clean.startsWith("forget ") || clean.startsWith("delete memory ") || clean.startsWith("remove memory ")) {
            val target = clean.removePrefix("forget ").removePrefix("delete memory ").removePrefix("remove memory ").trim()
            if (target.isNotBlank()) return MemoryIntentResult.DeletePrompt(target)
        }

        // 3. Question: Who is X?
        val whoMatch = Regex("^(who is|yaaru|do you know|who's) ([a-zA-Z0-9 ]+)\\??").find(clean)
        if (whoMatch != null) {
            val personName = whoMatch.groupValues[2].trim()
            return MemoryIntentResult.LookupQuery(personName, "PERSON")
        }

        // 4. Question: What do you remember about X?
        val whatMatch = Regex("^(what do you remember about|tell me about|remember about|what is my) ([a-zA-Z0-9 ]+)\\??").find(clean)
        if (whatMatch != null) {
            val subject = whatMatch.groupValues[2].trim()
            return MemoryIntentResult.LookupQuery(subject, "GENERAL")
        }

        // 5. Show my memories / What do you remember about me?
        if (clean == "what do you remember about me" || clean == "show my memories" || clean == "list my memories" || clean == "show memories") {
            return MemoryIntentResult.LookupQuery("", "GENERAL")
        }

        // 6. Save commands: "Remember that X is my Y"
        if (clean.startsWith("remember that ") || clean.startsWith("remember ") || clean.startsWith("save that ") || clean.startsWith("save ")) {
            val rawBody = clean.removePrefix("remember that ")
                .removePrefix("remember ")
                .removePrefix("save that ")
                .removePrefix("save ")
                .trim()

            // Check if person pattern: "Arun is my project partner" / "Arun from CSE is my classmate"
            val personPattern = Regex("^([a-zA-Z]+)(?: from ([a-zA-Z]+))? is my ([a-zA-Z ]+)").find(rawBody)
            if (personPattern != null) {
                val name = personPattern.groupValues[1].replaceFirstChar { it.uppercase() }
                val dept = personPattern.groupValues[2]
                val rel = personPattern.groupValues[3].replaceFirstChar { it.uppercase() }
                val content = if (dept.isNotBlank()) "$name from $dept is my $rel" else "$name is my $rel"
                return MemoryIntentResult.SaveMemoryPrompt(
                    title = name,
                    content = content,
                    category = MemoryCategory.PERSON_PROFILE,
                    personName = name,
                    relationship = rel
                )
            }

            // Check if object pattern: "my bike as my blue yamaha"
            if (rawBody.contains("bike") || rawBody.contains("car") || rawBody.contains("laptop") || rawBody.contains("guitar") || rawBody.contains("backpack")) {
                val words = rawBody.split(" ")
                val title = words.take(2).joinToString(" ").replaceFirstChar { it.uppercase() }
                return MemoryIntentResult.SaveMemoryPrompt(
                    title = title,
                    content = rawBody,
                    category = MemoryCategory.OBJECT_PROFILE
                )
            }

            // General fact / preference
            val title = rawBody.take(25).trim().replaceFirstChar { it.uppercase() }
            return MemoryIntentResult.SaveMemoryPrompt(
                title = title,
                content = rawBody,
                category = MemoryCategory.PERSONAL_INFO
            )
        }

        return MemoryIntentResult.None
    }
}
