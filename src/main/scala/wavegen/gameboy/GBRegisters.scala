package wavegen.gameboy

import chisel3._
import chisel3.util._

class GBRegisters extends Bundle {
	val NR10 = UInt(8.W)
	val NR11 = UInt(8.W)
	val NR12 = UInt(8.W)
	val NR13 = UInt(8.W)
	val NR14 = UInt(8.W)
	val NR21 = UInt(8.W)
	val NR22 = UInt(8.W)
	val NR23 = UInt(8.W)
	val NR24 = UInt(8.W)
	val NR30 = UInt(8.W)
	val NR31 = UInt(8.W)
	val NR32 = UInt(8.W)
	val NR33 = UInt(8.W)
	val NR34 = UInt(8.W)
	val NR41 = UInt(8.W)
	val NR42 = UInt(8.W)
	val NR43 = UInt(8.W)
	val NR44 = UInt(8.W)
	val NR50 = UInt(8.W)
	val NR51 = UInt(8.W)
	val NR52 = UInt(8.W)
	val WT0  = UInt(8.W)
	val WT1  = UInt(8.W)
	val WT2  = UInt(8.W)
	val WT3  = UInt(8.W)
	val WT4  = UInt(8.W)
	val WT5  = UInt(8.W)
	val WT6  = UInt(8.W)
	val WT7  = UInt(8.W)
	val WT8  = UInt(8.W)
	val WT9  = UInt(8.W)
	val WTA  = UInt(8.W)
	val WTB  = UInt(8.W)
	val WTC  = UInt(8.W)
	val WTD  = UInt(8.W)
	val WTE  = UInt(8.W)
	val WTF  = UInt(8.W)
}

object GBRegisters {
	def apply() = new GBRegisters
}
