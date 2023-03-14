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

class RAMData(dataWidth: Int, blockWidth: Int, bankWidth: Int) extends Bundle {
	val readData  = Flipped(DecoupledIO(UInt(dataWidth.W)))
	val writeData = Valid(UInt(dataWidth.W))
	val block     = Output(UInt(blockWidth.W))
	val bank      = Output(UInt(bankWidth.W))
	val cen       = Output(Bool())
}

class SDData extends Bundle {
	val ready      = Input(Bool())
	val writeReady = Input(Bool())
	val dataIn     = Flipped(Valid(UInt(8.W)))
	val doRead     = Output(Bool())
	val doWrite    = Output(Bool())
	val address    = Output(UInt(32.W))
	val dataOut    = Output(UInt(8.W))
}

class TOCRow extends Bundle {
	val apu     = UInt(8.W)
	val address = UInt(32.W)
	val name    = Vec(59, UInt(8.W))

	def valid: Bool = apu === 1.U || apu === 2.U
}

object NESButtons {
	def apply() = new NESButtons
}

object RAMData {
	def apply(dataWidth: Int = 64, blockWidth: Int = 22, bankWidth: Int = 4) =
		new RAMData(dataWidth, blockWidth, bankWidth)
}

object SDData {
	def apply() = new SDData
}

object TOCRow {
	def apply() = new TOCRow
}
