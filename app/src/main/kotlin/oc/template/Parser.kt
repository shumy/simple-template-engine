package oc.template

import java.io.File
import java.lang.RuntimeException

fun readFileAsLines(base: String, file: String): List<String>
    = File("$base/$file").useLines { it.toList() }

private val regexConfig = Regex("""#config\(([^)]*)\)""")
private val regexSet = Regex("""#set\(([^)]*)=([^)]*)\)""")
private val regexInclude = Regex("""#include\(([^)]*)\)""")
private val regexVar = Regex("""#var\(([^)]*)\)""")

class Parser(private val baseDir: String, private val parentVars: Map<String, String> = emptyMap()) {
    private val vars = mutableMapOf<String, String>()
    private val parsedLines = mutableListOf<String>()

    fun process(lines: List<String>): List<String> {
        lines.forEachIndexed { index, line -> parse(index+1, line) }
        return parsedLines
    }

    private fun parse(lineNumber: Int, line: String) {
        val spaces = countSpaces(line)
        val subLine = line.substring(spaces, line.length)

        when {
            subLine.startsWith("#config") -> processConfig(lineNumber, subLine)
            subLine.startsWith("#set") -> processSet(lineNumber, subLine)
            subLine.startsWith("#include") -> processInclude(lineNumber, spaces, subLine)
            subLine.contains("#var") -> processVar(lineNumber, line)
            else -> parsedLines.add(line)
        }
    }

    private fun countSpaces(line: String): Int {
        var spaces = 0
        for (char in line)
            if (char == ' ') spaces++ else break

        return spaces
    }

    private fun processConfig(lineNumber: Int, line: String) {
        val matches = regexConfig.findAll(line).toList()
        if (matches.size != 1)
            throw RuntimeException("Expecting one #config(<file>) expression for line $lineNumber")

        val file = matches.first().groupValues[1]
        val confLines = readFileAsLines(baseDir, file)

        confLines.forEach { processSet(lineNumber, it) }
    }

    private fun processSet(lineNumber: Int, line: String) {
        val matches = regexSet.findAll(line).toList()
        if (matches.size != 1)
            throw RuntimeException("Expecting one #set(<key> = <value>) expression for line $lineNumber")

        val key = matches.first().groupValues[1].trim()
        val value = matches.first().groupValues[2].trim()

        vars[key] = value.substring(1, value.length-1)
    }

    private fun processInclude(lineNumber: Int, spaces: Int, line: String) {
        val matches = regexInclude.findAll(line).toList()
        if (matches.size != 1)
            throw RuntimeException("Expecting one #include(<file>) expression for line $lineNumber")

        val file = matches.first().groupValues[1]

        val includeParser = Parser(baseDir, vars)
        includeParser.process(readFileAsLines(baseDir, file))
        val includeLines = includeParser.parsedLines.map { "${" ".repeat(spaces)}$it" }

        parsedLines.addAll(includeLines)
    }

    private fun processVar(lineNumber: Int, line: String) {
        val matches = regexVar.findAll(line)
        matches.ifEmpty { throw RuntimeException("Expecting #var(<var>) expressions for line $lineNumber") }

        val keys = matches.map { it.groupValues[1] }
        val maps = keys.map { it to (vars[it] ?: parentVars[it] ?: throw RuntimeException("No value found for #var($it)")) }.toMap()

        var replacedLine = line
        maps.forEach { (key, value) -> replacedLine = replacedLine.replace("#var($key)", value) }

        parsedLines.add(replacedLine)
    }
}