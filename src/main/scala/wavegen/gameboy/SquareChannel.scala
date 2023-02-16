package wavegen.gameboy

import chisel3._
import chisel3.util._

class SquareChannel extends Module {
	val io = IO(new Bundle {
		val tick      = Input(Bool())
		val registers = Input(Registers())
		val out       = Output(UInt(4.W))
	})
}
