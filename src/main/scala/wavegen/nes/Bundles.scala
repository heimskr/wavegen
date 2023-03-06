package wavegen.nes

import chisel3._
import chisel3.util._

class ChannelIO extends Bundle {
	val ticks     = Input(Ticks())
	val registers = Input(NESRegisters())
	val writes    = Input(PulseWrites())
	val out       = Output(UInt(4.W))
}

class Ticks extends Bundle {
	val cpu     = Bool()
	val apu     = Bool()
	val quarter = Bool()
	val half    = Bool()
}

class PulseWrites extends Bundle {
	val sweeper = Bool()
	val length  = Bool()
}

class TriangleWrites extends Bundle {
	val counterReload = Bool()
}

object ChannelIO      { def apply() = new ChannelIO      }
object Ticks          { def apply() = new Ticks          }
object PulseWrites    { def apply() = new PulseWrites    }
object TriangleWrites { def apply() = new TriangleWrites }
