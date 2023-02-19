package wavegen

import chisel3._
import chisel3.util._

class SquareGenExternal(width: Int, waveformWidth: Int = 2) extends Module {
	require(1 <= waveformWidth)

	val io = IO(new Bundle {
		val tick  = Input(Bool())
		val max   = Input(UInt(width.W))
		val wave  = Input(UInt(waveformWidth.W))
		val out   = Output(UInt(width.W))
	})

	val (waveCounter, waveWrap) = Counter(0 until waveformWidth, io.tick)

	when (io.wave(waveCounter)) {
		io.out := io.max
	} .otherwise {
		io.out := 0.U
	}
}
