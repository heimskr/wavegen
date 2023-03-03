package wavegen.nes

import chisel3._
import chisel3.util._

class ChannelIO extends Bundle {
	val tick      = Input(Bool())
	val registers = Input(NESRegisters())
	val out       = Output(UInt(4.W))
}
