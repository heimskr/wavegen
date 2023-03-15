package wavegen

import chisel3._
import chisel3.util._

/** Makes a pulse signal from a fast clock available on a slow clock. */
class FastPulseDomainCrosser extends BlackBox {
	val io = IO(new Bundle {
		val clock     = Input(Clock())
		val reset     = Input(Reset())
		val slowClock = Input(Clock())
		val pulseIn   = Input(Bool())
		val pulseOut  = Output(Bool())
	})
}

object FastPulseDomainCrosser {
	def apply(fastClock: Clock, reset: Reset, slowClock: Clock, pulseIn: Bool): Bool = {
		val module = Module(new FastPulseDomainCrosser)
		module.io.clock     := fastClock
		module.io.reset     := reset
		module.io.slowClock := slowClock
		module.io.pulseIn   := pulseIn
		module.io.pulseOut
	}
}
