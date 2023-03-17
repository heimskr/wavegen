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

class BoolMonitor extends Module {
	val io = IO(new Bundle {
		val in      = Input(Bool())
		val changed = Output(Bool())
		val risen   = Output(Bool())
	})

	val previous = RegNext(io.in)
	io.changed := io.in =/= previous
	io.risen   := io.in && !previous
}

object Monitor {
	def apply(value: UInt): Bool = {
		val module = Module(new Monitor(chiselTypeOf(value)))
		module.io.in := value
		module.io.changed
	}
}

object BoolMonitor {
	def apply(value: Bool): Bool = {
		val module = Module(new BoolMonitor)
		module.io.in := value
		module.io.risen
	}
}
