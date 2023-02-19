package wavegen.gameboy

import chisel3._
import chisel3.util._

class SquareChannelIO extends Bundle {
	val tick      = Input(Bool())
	val registers = Input(Registers())
	val out       = Valid(UInt(4.W))
}
