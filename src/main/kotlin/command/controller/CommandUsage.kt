package org.celery.command.controller

data class CommandUsage(
    val basicUsage: CommandBasicUsage,
    val enable: Boolean,
    val havePermission: Boolean,
    val others: String = ""
) {
    data class CommandParam(
        val name: String,
        val optional: Boolean = false
    ) {
        fun render(): String {
            return if (optional)
                "<optional>$name</optional>"
            else
                "<must>$name</must>"
        }
    }

    fun renderToLi(): String {
        var className = ""
        if (!enable) className = "disable "
        if (!havePermission) className += "no-permission"
        val DivCommandName = """
            <div class="command-name">
                ${basicUsage.commandNameDisplay.replaceParam()}
                ${basicUsage.params.joinToString("") { it.render() }}
                <span>${others.let { it.ifBlank { "" } }}</span>
            </div>
        """.trimIndent()
        val DivDescription = if (basicUsage.description != "no description available") """
            <div class="command-description">
                ${(basicUsage.description).trimIndent().replace("\n", "<br>")}
            </div>
        """.trimIndent() else ""
        val DivUsage = if (basicUsage.example.isNotBlank()) """
            <div class="command-usage">
                ${basicUsage.example.trimIndent().replace("\n", "<br>")}
            </div>
        """.trimIndent() else ""
        return """
            <li class="$className">
            $DivCommandName
            $DivDescription
            $DivUsage
        </li>
        """.trimIndent()
    }
}

private fun String.replaceParam(): String {
        return replace("<","<<")
        .replace(">",">>")
        .replace("<<","<must>")
        .replace(">>","</must>")
        .replace("[","<optional>")
        .replace("]","</optional>")
}

data class CommandBasicUsage(
    val commandNameDisplay: String,
    val params: List<CommandUsage.CommandParam>,
    val description: String,
    val superUserUsage: String,
    val example: String,
    val commandId: String
)