package wavegen.nes

import chisel3._
import chisel3.util._

class ChannelIO extends Bundle {
	val ticks     = Input(Ticks())
	val registers = Input(NESRegisters())
	val out       = Output(UInt(4.W))
}

class Ticks extends Bundle {
	val apu     = Bool()
	val quarter = Bool()
	val half    = Bool()
}

class PulseWrites extends Bundle {
	val dutyCycle = Bool()
	val sweeper   = Bool()
	val length    = Bool()
}

object ChannelIO   { def apply() = new ChannelIO   }
object Ticks       { def apply() = new Ticks       }
object PulseWrites { def apply() = new PulseWrites }
