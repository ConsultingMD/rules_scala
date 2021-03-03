package scripts

import org.scalajs.linker.interface.{ModuleKind, StandardConfig}

case class ScalaJSLinkRequest(
  jsOutput: String,
  mainClass: String,
  mainMethod: String,
  classPath: Seq[String],
  config: StandardConfig
)

object ScalaJSLinkRequest {
  def from(args: Array[String]): ScalaJSLinkRequest = {
    // TODO: determine what linker configuration values we'd like to support.
    ScalaJSLinkRequest(
      args(0),
      args(1),
      args(2),
      args(3).split(":").toSeq,
      config = StandardConfig()
        .withOptimizer(true)
        .withParallel(true)
        .withSourceMap(false)
        .withClosureCompilerIfAvailable(true)
        .withModuleKind(ModuleKind.CommonJSModule)
    )
  }
}
