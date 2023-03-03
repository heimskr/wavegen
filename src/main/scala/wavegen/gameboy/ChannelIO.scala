package wavegen.gameboy

import chisel3._
import chisel3.util._

class GBChannelIO extends Bundle {
	val tick      = Input(Bool())
	val registers = Input(GBRegisters())
	val out       = Output(UInt(4.W))
}
