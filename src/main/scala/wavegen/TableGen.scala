package wavegen

import chisel3._
import chisel3.util._

class TableGen[T <: Generator](gen: T, period: Int, resolution: Int)(implicit clockFreq: Int) extends Module {
	require(1 < period)
	require(1 < resolution)
	require(1 < clockFreq)
	val clockWidth = log2Ceil(clockFreq + 1)
	val outWidth = log2Ceil(resolution + 1).W

	val io = IO(new Bundle {
		val pause = Input(Bool())
		val freq = Input(UInt(clockWidth.W))
		val out = Output(UInt(outWidth))
	})

	val clocker = Module(new Clocker)
	clocker.io.enable := !io.pause
	clocker.io.freq.bits := io.freq * period.U
	clocker.io.freq.valid := io.freq * period.U

	val (counter, wrap) = Counter(clocker.io.tick, period)

	val list = List.tabulate(period)(i => (gen(i.doubleValue / period) * resolution).toInt.U(outWidth))
	val data = VecInit(list)

	io.out := data(counter)
}
