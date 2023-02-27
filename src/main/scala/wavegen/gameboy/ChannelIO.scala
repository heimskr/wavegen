package wavegen.gameboy

import chisel3._
import chisel3.util._

class ChannelIO extends Bundle {
	val tick      = Input(Bool())
	val registers = Input(Registers())
	val out       = Output(UInt(4.W))
}
