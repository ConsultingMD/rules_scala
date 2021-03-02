package scripts

import java.nio.file.Paths
import java.nio.file.Files

import org.scalajs.linker.interface.{ModuleInitializer, StandardConfig}
import org.scalajs.linker.{MemOutputDirectory, PathIRContainer, StandardImpl}
import org.scalajs.logging.{Level, ScalaConsoleLogger}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import io.bazel.rulesscala.worker.Worker

object ScalaJSLinker extends Worker.Interface {
  def main(args: Array[String]): Unit = Worker.workerMain(args, ScalaJSLinker)

  override def work(args: Array[String]): Unit = {
    val linkRequest = ScalaJSLinkRequest.from(args)

    val linkerConfig = linkRequest.config
    val inputFiles = linkRequest.classPath.map(path => Paths.get(".", path))

    val linker = StandardImpl.linker(linkerConfig)
    val moduleInitializers = Seq(
      ModuleInitializer.mainMethod(linkRequest.mainClass, linkRequest.mainMethod)
    )
    val outputDirectory = MemOutputDirectory()
    val cache = StandardImpl.irFileCache().newCache

    val result = PathIRContainer
      .fromClasspath(inputFiles)
      .map(_._1)
      .flatMap(cache.cached _)
      .flatMap(
        linker.link(_, moduleInitializers, outputDirectory, new ScalaConsoleLogger(Level.Error))
      )

    val report = Await.result(result, Duration.Inf)

    if (report.publicModules.size != 1)
      throw new AssertionError(s"got other than 1 module: $report")

    val jsOutput = outputDirectory
      .content(report.publicModules.head.jsFileName)
      .getOrElse(
        throw new RuntimeException("compiled js not found")
      )

    Files.write(Paths.get(".", linkRequest.jsOutput), jsOutput)
  }
}
