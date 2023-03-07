package wavegen.misc

import chisel3._
import chisel3.util._
import scala.math.{sin, floor, Pi}

case class WavyTextOpts(text: String, centerX: Boolean, xOffset: Int, yOffset: Int, shift: Int, speed: Int = 32, waveFactor: Int = 4)

class WavyText(opts: WavyTextOpts, xWidth: Int = 11, yWidth: Int = 10) extends Module {
	val io = IO(new Bundle {
		val x   = Input(UInt(xWidth.W))
		val y   = Input(UInt(yWidth.W))
		val out = Output(Bool())
	})

	val undulate = Module(new wavegen.StaticClocker(opts.speed, 74_250_000, true, "WavyClocker" + opts.speed))
	undulate.io.enable := true.B
	val font      = Module(new wavegen.presentation.Font)
	val shift     = opts.shift
	val text      = VecInit(opts.text.toList.map(_.U(8.W)))
	val width     = text.length * 8
	val height    = 8
	val top       = opts.yOffset
	val x         = (
		if (opts.centerX)
			io.x - (opts.xOffset - (width << (shift - 1))).U
		else
			io.x - opts.xOffset.U
	) >> shift.U
	val charIndex = x >> 3.U
	val char      = text(charIndex)

	val resolution = 128
	val sines = VecInit.tabulate(resolution)(x => floor((sin(x * 8 * Pi / resolution) + 1) * (opts.waveFactor << shift)).toInt.U)

	val counter = RegInit(0.U(log2Ceil(resolution - text.length).W))
	when (undulate.io.tick) {
		when (counter >= (resolution - 1).U) {
			counter := 0.U
		} .otherwise {
			counter := counter + 1.U
		}
	}

	val y = (io.y - top.U - sines((counter + charIndex)(6, 0))) >> shift.U

	// The RegNexts here are a WNS hack that appears to have no impact on the visuals.
	font.io.char := RegNext(char)
	font.io.x    := RegNext(x(2, 0))
	font.io.y    := RegNext(y(2, 0))

	io.out := 0.U <= x && x < width.U && 0.U <= y && y < height.U && font.io.out
}