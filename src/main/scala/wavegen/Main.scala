package wavegen

import chisel3._
import chisel3.stage._

object Main extends scala.App {
	(new ChiselStage).emitVerilog(new Mixer(4, 16, 10), args)
}
