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
	val sSumming :: sAdjusting :: sDone :: Nil = Enum(3)

	val io = IO(new Bundle {
		val in  = Vec(channelCount, Flipped(Decoupled(FixedPoint(width.W, 0.BP))))
		val out = Decoupled(FixedPoint(width.W, 0.BP))
		val debug = new MixerDebug(channelCount, width)
	})

	val bigger = width + log2Ceil(channelCount)
	
	val state = RegInit(sSumming)
	io.debug.state := state
	
	io.out.valid := false.B
	io.out.bits := 0.F(0.BP)
	
	io.in.foreach { _.ready := true.B }
	
	val summer = Module(new FPSummer(channelCount, width))
	val maxReg = RegInit(summer.makeOut(0))

	io.debug.max := maxReg

	for (i <- 0 until channelCount) {
		summer.io.in(i) := io.in(i).bits
	}

	val allValid = io.in.foldLeft(true.B)(_ && _.valid)

	val memory = Mem(memorySize, summer.outType)

	val enableIndex = Reg(Bool())
	val resetIndex = Reg(Bool())
	val (index, indexWrap) = Counter(0 until memorySize, enableIndex, resetIndex)

	val maxPossible = Fill(width, 1.U(1.W))

	enableIndex := false.B
	resetIndex  := false.B

	when (state === sSumming) {
		when (allValid) {
			memory(index) := summer.io.out

			// FixedPoints appear to be always signed.
			when (maxReg.asUInt < summer.io.out.asUInt) {
				maxReg := summer.io.out
			}

			when (indexWrap) {
				resetIndex := true.B
				when (maxPossible < maxReg.asUInt) {
					state := sAdjusting
				} .otherwise {
					state := sDone
				}
			} .otherwise {
				enableIndex := true.B
			}
		}
	} .elsewhen (state === sAdjusting) {
		val newValue = (memory(index).asUInt * maxPossible / maxReg.asUInt).asFixedPoint(0.BP)
		memory(index) := newValue
		io.out.valid := true.B
		io.out.bits := newValue

		when (indexWrap) {
			resetIndex := true.B
			state := sDone
		} .otherwise {
			enableIndex := true.B
		}
	} .elsewhen (state === sDone) {
		resetIndex := true.B
		enableIndex := false.B
	}
}
