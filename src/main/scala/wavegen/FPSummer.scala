package wavegen

import chisel3._
import chisel3.util._
import scala.collection.immutable.ListMap
import chisel3.experimental.FixedPoint

class FPSummer(inputCount: Int, width: Int, point: Int = 0) extends Module {
	require(0 < inputCount)
	val extra = log2Ceil(inputCount)
	val bigPoint = point + extra
	val bigWidth = width + extra
	val outType = FixedPoint(bigWidth.W, bigPoint.BP)

	val io = IO(new Bundle {
		val in = Input(Vec(inputCount, FixedPoint(width.W, point.BP)))
		val out = Output(outType)
	})

	def makeOut(value: Double) = value.F(bigWidth.W, bigPoint.BP)

	if (inputCount == 1) {
		io.out := io.in(0)
	} else {
		io.out := io.in.foldLeft(.0.F(bigWidth.W, bigPoint.BP)) { (sum, in) =>
			sum + Cat(0.U(extra.W), in).asFixedPoint(bigPoint.BP)
		}
	}
}
