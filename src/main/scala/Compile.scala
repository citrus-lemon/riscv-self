package rself

import java.io.{File, FileWriter}

object Compile {
  val modules:Map[String, () => chisel3.Module] = Map(
    "RegFile" -> (() => new RegFile()),
    "ALU"     -> (() => new ALU()),
    "Control" -> (() => new Control())
  )

  def dump(dir:File, mod: () => chisel3.Module) = {
    val chirrtl = firrtl.Parser.parse(chisel3.Driver.emit(mod))
    val writer = new FileWriter(new File(dir, s"${chirrtl.main}.fir"))
    writer write chirrtl.serialize
    writer.close

    val verilog = new FileWriter(new File(dir, s"${chirrtl.main}.v"))
    new firrtl.VerilogCompiler compile (
      firrtl.CircuitState(chirrtl, firrtl.ChirrtlForm), verilog)
    verilog.close
  }

  def main(args: Array[String]): Unit = {
    val dir = new File(args(0)) ; dir.mkdirs
    if (args.length >= 2) {
      modules.get(args(1)) match {
        case Some(mod) =>
          dump(dir, mod)
        case _ =>
          println(s"no module names ${args(1)}")
      }
    }
    else {
      modules.foreach {
        case (key, value) => dump(dir, value)
      }
    }
  }
}