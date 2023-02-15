package wavegen.gameboy

import chisel3._
import chisel3.util._

class GameBoy(implicit clockFreq: Int) extends Module {
	val io = IO(new Bundle {
		val audio = Output(UInt(4.W))
	})

	
}