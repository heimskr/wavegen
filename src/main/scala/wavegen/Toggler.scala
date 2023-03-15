package wavegen

import chisel3._
import chisel3.util._

class Toggler extends Module {
	val io = IO(new Bundle {
		val in  = Input(Bool())
		val out = Output(Bool())
	})

	val out = RegInit(false.B)
	io.out := out

	when (io.in) {
		out := !out
	}
}

object Toggler {
	def apply(signal: Bool): Bool = {
		val module = Module(new Toggler)
		module.io.in := signal
		module.io.out
	}
}
