package wavegen.misc

import chisel3._
import chisel3.util._

class Monitor(dataType: UInt) extends Module {
	val io = IO(new Bundle {
		val in      = Input(dataType)
		val changed = Output(Bool())
	})

	io.changed := io.in =/= RegNext(io.in)
}

object Monitor {
	def apply(value: UInt): Bool = {
		val module = Module(new Monitor(chiselTypeOf(value)))
		module.io.in := value
		module.io.changed
	}
}
