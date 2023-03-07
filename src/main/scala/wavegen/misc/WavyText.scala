package wavegen.misc

import chisel3._
import chisel3.util._
import scala.math.{sin, floor, Pi}

case class WavyTextOpts(text: String, centerX: Boolean, centerY: Boolean, xOffset: Int, yOffset: Int, shift: Int, speed: Int = 32, waveFactor: Int = 4, scrunch: Int = 5, antiscrunch: Int = 6)

class WavyText(opts: WavyTextOpts, xWidth: Int = 11, yWidth: Int = 10, moduleName: String = "") extends Module {
	override val desiredName =
		if (moduleName.isEmpty())
			("WavyText_"
				+ opts.text.filter(_.isLetterOrDigit) + "_"
				+ opts.text.hashCode() + "_"
				+ (if (opts.centerX) "CX" else "")
				+ (if (opts.centerY) "CY" else "")
				+ opts.xOffset + "x"
				+ opts.yOffset + "y"
				+ opts.shift + "sh"
				+ opts.speed + "sp"
				+ opts.waveFactor + "wf"
				+ opts.scrunch + "sc"
				+ xWidth + "xw"
				+ yWidth + "yw")
		else
			moduleName

	val io = IO(new Bundle {
		val x   = Input(UInt(xWidth.W))
		val y   = Input(UInt(yWidth.W))
		val out = Output(Bool())
	})

	val undulate = Module(new wavegen.StaticClocker(opts.speed, 74_250_000, true))
	undulate.io.enable := true.B
	val font   = Module(new wavegen.presentation.Font)
	val shift  = opts.shift
	val text   = VecInit(opts.text.toList.map(_.U(8.W)))
	val width  = text.length * 8
	val height = 8
	val left   = opts.xOffset
	val top    = opts.yOffset
	val x      = (
		if (opts.centerX)
			io.x - (left - (width << (shift - 1))).U
		else
			io.x - left.U
	) >> shift.U
	val charIndex = x >> 3.U
	val char      = text(charIndex)

	val scrunch     = 1 << opts.scrunch
	val resolution  = 8 * scrunch
	val sines       = Seq.tabulate(resolution)(x => floor((sin(x * (scrunch >> opts.antiscrunch) * Pi) + 1) * (opts.waveFactor << shift)).toInt)
	val sinesVec    = VecInit(sines.map(_.U))
	val average     = sines.sum / resolution
	val sineMaxBit  = log2Ceil(resolution) - 1

	val counter = RegInit(0.U(log2Ceil(resolution - text.length).W))
	when (undulate.io.tick) {
		when (counter >= (resolution - 1).U) {
			counter := 0.U
		} .otherwise {
			counter := counter + 1.U
		}
	}

	val y = (
		if (opts.centerY)
			(io.y - (top - average - (height << (shift - 1))).U - sinesVec((counter + charIndex)(sineMaxBit, 0)))
		else
			(io.y - top.U - sinesVec((counter + charIndex)(sineMaxBit, 0)))
	) >> shift.U

	// The RegNexts here are a WNS hack that appears to have no impact on the visuals.
	font.io.char := RegNext(char)
	font.io.x    := RegNext(x(2, 0))
	font.io.y    := RegNext(y(2, 0))

	io.out := 0.U <= x && x < width.U && 0.U <= y && y < height.U && font.io.out
}
