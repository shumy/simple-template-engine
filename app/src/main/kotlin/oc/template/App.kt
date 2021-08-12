package oc.template

import picocli.CommandLine
import picocli.CommandLine.Option
import picocli.CommandLine.Command
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(name = "ste", mixinStandardHelpOptions = true, version = ["ste 1.0"], description = ["Simple Template Engine (ste) that preserves indentation."])
class Input : Callable<Int> {
    @Option(names = ["-d", "--directory"], description = ["The working directory where to find template files"])
    var workingDir = System.getProperty("user.dir")!!

    @Option(names = ["-f", "--file"], description = ["The template file to process"])
    var file = "build.yaml"

    override fun call(): Int {
        //println("Working Directory: $workingDir")
        //println("Processing: $file")

        val lines = readFileAsLines(workingDir, file)
        Parser(workingDir).process(lines).forEach {
            println(it)
        }

        return 0
    }
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(Input()).execute(*args))
