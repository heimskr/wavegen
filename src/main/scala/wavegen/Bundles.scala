package wavegen

import chisel3._
import chisel3.util._

class NESButtons extends Bundle {
	val a      = Bool()
	val b      = Bool()
	val select = Bool()
	val start  = Bool()
	val up     = Bool()
	val down   = Bool()
	val left   = Bool()
	val right  = Bool()
}

class RAMBundle(dataWidth: Int, blockWidth: Int, bankWidth: Int) extends Bundle {
	val readData  = Flipped(DecoupledIO(UInt(dataWidth.W)))
	val writeData = Valid(UInt(dataWidth.W))
	val block     = Output(UInt(blockWidth.W))
	val bank      = Output(UInt(bankWidth.W))
	val cen       = Output(Bool())
}

object NESButtons {
	def apply() = new NESButtons
}

object RAMBundle {
	def apply(dataWidth: Int = 64, blockWidth: Int = 22, bankWidth: Int = 4) =
		new RAMBundle(dataWidth, blockWidth, bankWidth)
}
