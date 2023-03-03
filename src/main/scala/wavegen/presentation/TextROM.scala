package wavegen.presentation

import chisel3._
import chisel3.util._

class TextROM extends BlackBox {
	val io = IO(new Bundle {
		val clka  = Input(Clock())
		val addra = Input(UInt(16.W))
		val douta = Output(UInt(8.W))
	})
}
