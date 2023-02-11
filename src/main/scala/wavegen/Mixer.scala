package wavegen

import chisel3._
import chisel3.experimental._
import chisel3.util._
import scala.collection.immutable.ListMap

class MixerDebug(channelCount: Int, width: Int) extends Bundle {
	private val extra = log2Ceil(channelCount)
	val state = Output(UInt(log2Ceil(2).W))
	val max = Output(FixedPoint((width + extra).W, extra.BP))
}

class Mixer(channelCount: Int, width: Int, memorySize: Int) extends Module {
	val sInit :: sSumming :: Nil = Enum(2)

	val io = IO(new Bundle {
		val in  = Vec(channelCount, Flipped(Decoupled(FixedPoint(width.W, 0.BP))))
		val out = Decoupled(FixedPoint(width.W, 0.BP))
		val debug = new MixerDebug(channelCount, width)
	})

	val bigger = width + log2Ceil(channelCount)
	
	val state = RegInit(sInit)
	io.debug.state := state
	
	io.out.valid := false.B
	io.out.bits := .0.F(0.BP)
	
	io.in.foreach { _.ready := true.B }
	
	val summer = Module(new FPSummer(channelCount, width))
	val maxReg = RegInit(summer.makeOut(0.0))

	io.debug.max := maxReg

	for (i <- 0 until channelCount) {
		summer.io.in(i) := io.in(i).bits
	}

	val allValid = io.in.foldLeft(true.B)(_ && _.valid)

	val memory = Reg(Vec(memorySize, summer.outType))
	val index = RegInit(0.U(log2Ceil(memorySize).W))

	when (state === sInit) {
		when (allValid) {
			memory(index) := summer.io.out

			when (maxReg.asUInt < summer.io.out.asUInt) {
				maxReg := summer.io.out
			}

			when (index === (memorySize - 1).U) {
				index := 0.U
				state := sSumming
			} .otherwise {
				index := index + 1.U
			}
		}
	}
}
