package wavegen

import chisel3._
import chisel3.util._

/** Makes a pulse signal from a fast clock available on a slow clock. */
class FastPulseDomainCrosser extends Module {
	val io = IO(new Bundle {
		val slowClock = Input(Clock())
		val pulseIn   = Input(Bool())
		val pulseOut  = Output(Bool())
	})

	val reg = Reg(Bool())

	when (io.pulseIn) {
		reg := true.B
	}

	withClock (io.slowClock) {
		reg := false.B
	}

	io.pulseOut := reg
}

object FastPulseDomainCrosser {
	def apply(slowClock: Clock, pulseIn: Bool): Bool = {
		val module = Module(new FastPulseDomainCrosser)
		module.io.slowClock := slowClock
		module.io.pulseIn   := pulseIn
		module.io.pulseOut
	}
}
